package com.wingedsheep.ai.arena

import com.wingedsheep.ai.engine.buildSeededSealedDeck
import com.wingedsheep.ai.engine.safeFallbackAction
import com.wingedsheep.engine.core.ActionProcessor
import com.wingedsheep.engine.core.GameConfig
import com.wingedsheep.engine.core.GameInitializer
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.core.PlayerConfig
import com.wingedsheep.engine.core.SubmitDecision
import com.wingedsheep.engine.legalactions.EnumerationMode
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.mtg.sets.MtgSetCatalog
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.model.Rarity
import io.kotest.core.spec.style.FunSpec
import java.io.File
import java.util.concurrent.Executors
import kotlin.random.Random

/**
 * Plays AI-vs-AI games with sealed decks from *random sets* (a different set per seat) and writes a
 * human-readable log per game, for reading where the AI misplays. Off unless `-DgameLog=true`.
 *
 * ```
 * just ai-game-logs 24            # or, by hand:
 * scripts/gradle-locked :ai:test --tests "*.GameLogBenchmark" -DgameLog=true \
 *     -DgameLogGames=20 -DgameLogAgent=production-candidate-expiring -DgameLogDir=/tmp/logs
 * ```
 */
class GameLogBenchmark : FunSpec({
    val enabled = System.getProperty("gameLog") == "true"

    test("game logs across random sets").config(enabled = enabled) {
        val games = System.getProperty("gameLogGames")?.toIntOrNull() ?: 20
        val seed = System.getProperty("gameLogSeed")?.toLongOrNull() ?: 20261009L
        val agent = ArenaAgents.resolve(System.getProperty("gameLogAgent") ?: "production-candidate-expiring")
        val outDir = File(System.getProperty("gameLogDir") ?: "build/game-logs").apply { mkdirs() }

        val eligible = MtgSetCatalog.all.filter { set ->
            !set.incomplete && set.cards.count { !it.typeLine.isBasicLand && it.metadata.rarity == Rarity.COMMON } >= 40 &&
                set.cards.any { it.metadata.rarity == Rarity.RARE }
        }
        println("Eligible sets: ${eligible.size}")
        val registry = harnessRegistry(MtgSetCatalog.all)

        val pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors().coerceAtMost(8))
        val futures = (1..games).map { g ->
            pool.submit<String> {
                val rng = Random(seed * 1000 + g)
                val sets = List(2) { eligible[rng.nextInt(eligible.size)] }
                val decks = sets.map { buildSeededSealedDeck(it.cards, rng) }
                val log = StringBuilder()
                log.appendLine("GAME $g  seed=${seed * 1000 + g}  agent=${agent.name}")
                sets.forEachIndexed { i, s ->
                    log.appendLine("Seat$i deck (${s.code} ${s.displayName}): " +
                        decks[i].cards.groupingBy { it }.eachCount().entries.joinToString(", ") { "${it.value}x ${it.key}" })
                }
                val summary = try {
                    playLogged(registry, agent, decks, seed * 1000 + g, log)
                } catch (e: Throwable) {
                    "EXCEPTION ${e::class.simpleName}: ${e.message}".also { log.appendLine(it) }
                }
                File(outDir, "game-%02d.log".format(g)).writeText(log.toString())
                "game $g ${sets[0].code} vs ${sets[1].code}: $summary"
            }
        }
        futures.forEach { println(it.get()) }
        pool.shutdown()
        println("Logs in ${outDir.absolutePath}")
    }
})

private fun playLogged(
    registry: CardRegistry,
    agent: ArenaAgent,
    decks: List<com.wingedsheep.sdk.model.Deck>,
    seed: Long,
    log: StringBuilder,
): String {
    val processor = ActionProcessor(registry)
    val enumerator = LegalActionEnumerator.create(registry)
    val init = GameInitializer(registry).initializeGame(
        GameConfig(
            players = decks.mapIndexed { seat, deck -> PlayerConfig("Seat$seat", deck) },
            skipMulligans = true,
            startingPlayerIndex = 0,
            seed = seed,
        )
    )
    val seatIds = init.state.turnOrder
    val decklists = seatIds.mapIndexed { i, id -> id to decks[i].cards.groupingBy { it }.eachCount() }.toMap()
    val players = seatIds.map { agent.createPlayer(registry, it, decklists) }
    val seatOf = seatIds.withIndex().associate { (i, id) -> id to i }

    var state: GameState = init.state
    val uuid = Regex("\\be\\d+\\b")
    // Drop the default-valued fields of the action data classes so a line shows only what was chosen.
    val noise = Regex(", \\w+=(?:null|false|\\[]|\\{}|0|1|AutoPay)(?=[,)])")

    fun nameOf(s: GameState, id: EntityId): String {
        seatOf[id]?.let { return "Seat$it" }
        val e = s.getEntity(id) ?: return "?$id"
        val card = e.get<CardComponent>() ?: return "obj"
        val p = s.projectedState
        return if (p.isCreature(id) && id in s.getBattlefield()) "${card.name}(${p.getPower(id)}/${p.getToughness(id)})" else card.name
    }
    fun humanize(s: GameState, text: String) =
        uuid.replace(noise.replace(noise.replace(text, ""), "")) { m -> nameOf(s, EntityId(m.value)) }

    fun perm(s: GameState, id: EntityId): String {
        val e = s.getEntity(id)!!
        val card = e.get<CardComponent>()!!
        val p = s.projectedState
        val sb = StringBuilder(card.name)
        if (p.isCreature(id)) {
            sb.append(" ${p.getPower(id)}/${p.getToughness(id)}")
            val kw = p.getKeywords(id)
            if (kw.isNotEmpty()) sb.append(" [${kw.joinToString(",") { it.lowercase() }}]")
            e.get<DamageComponent>()?.let { sb.append(" dmg${it.amount}") }
        }
        if (e.has<TappedComponent>()) sb.append(" (T)")
        return sb.toString()
    }

    fun board(s: GameState) {
        seatIds.forEachIndexed { i, pid ->
            val bf = s.getBattlefield(pid)
            val lands = bf.filter { s.getEntity(it)?.get<CardComponent>()?.typeLine?.isLand == true }
            val others = bf - lands.toSet()
            val hand = s.getZone(pid, Zone.HAND).mapNotNull { s.getEntity(it)?.get<CardComponent>() }
            log.appendLine("    Seat$i life=${s.lifeTotal(pid)} lib=${s.getZone(pid, Zone.LIBRARY).size} " +
                "gy=${s.getZone(pid, Zone.GRAVEYARD).size} lands=${lands.size}(${lands.count { s.getEntity(it)!!.has<TappedComponent>() }}T)")
            log.appendLine("      board: ${others.joinToString("; ") { perm(s, it) }}")
            log.appendLine("      hand: ${hand.joinToString("; ") { "${it.name} ${it.manaCost}" }}")
        }
    }

    var lastTurn = -1
    var actions = 0
    var illegal = 0
    while (!state.gameOver && state.turnNumber < 60 && actions < 6000) {
        if (state.turnNumber != lastTurn) {
            lastTurn = state.turnNumber
            log.appendLine("\n=== TURN ${state.turnNumber} — active Seat${seatOf[state.activePlayerId]} ===")
            board(state)
        }
        val decision = state.pendingDecision
        if (decision != null) {
            actions++
            val response = players[seatOf[decision.playerId]!!].respondToDecision(state, decision)
            log.appendLine("  [${state.step}] Seat${seatOf[decision.playerId]} DECISION ${decision::class.simpleName}: " +
                humanize(state, decision.toString()).take(400))
            log.appendLine("      -> ${humanize(state, response.toString()).take(300)}")
            val r = processor.process(state, SubmitDecision(decision.playerId, response)).result
            if (r.error != null) { log.appendLine("  DECISION ERROR ${r.error}"); return "decisionError" }
            state = r.state
            continue
        }
        val pp = state.priorityPlayerId ?: return "noPriority"
        actions++
        val action = players[seatOf[pp]!!].chooseAction(state)
        if (action is PassPriority) {
            // A pass in our own main phase with an empty stack while holding playable cards is the
            // most common "did nothing" misplay — record what was on offer.
            if (pp == state.activePlayerId && state.stack.isEmpty() && state.step.name.contains("MAIN")) {
                val offered = enumerator.enumerate(state, pp, EnumerationMode.ACTIONS_ONLY)
                    .filter { it.affordable && it.actionType != "PassPriority" && !it.description.contains(": Add {") }
                    .map { humanize(state, it.description) }
                    .distinct()
                if (offered.isNotEmpty()) log.appendLine("  [${state.step}] Seat${seatOf[pp]} passes; had: ${offered.joinToString(" | ").take(500)}")
            }
        } else {
            val stackTop = state.stack.lastOrNull()?.let { " (stack top: ${nameOf(state, it)})" }.orEmpty()
            log.appendLine("  [${state.step}] Seat${seatOf[pp]}: ${humanize(state, action.toString()).take(500)}$stackTop")
        }
        val r = processor.process(state, action).result
        state = if (r.error != null) {
            illegal++
            log.appendLine("  ILLEGAL: ${r.error}")
            processor.process(state, safeFallbackAction(state, pp, enumerator)).result.state
        } else r.state
    }
    val winner = state.winnerId?.let { seatOf[it] }
    log.appendLine("\n=== END turn=${state.turnNumber} winner=${winner?.let { "Seat$it" } ?: "none"} life=${seatIds.map { state.lifeTotal(it) }} illegal=$illegal ===")
    board(state)
    return "winner=${winner ?: "none"} turns=${state.turnNumber} illegal=$illegal"
}
