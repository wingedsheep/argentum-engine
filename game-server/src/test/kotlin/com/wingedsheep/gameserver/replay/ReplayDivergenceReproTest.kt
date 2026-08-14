package com.wingedsheep.gameserver.replay

import com.wingedsheep.ai.engine.buildHeuristicSealedDeck
import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.gameserver.session.GameSession
import com.wingedsheep.gameserver.session.PlayerSession
import com.wingedsheep.mtg.sets.MtgSetCatalog
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.model.Rarity
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.springframework.web.socket.WebSocketSession
import java.time.Instant
import kotlin.random.Random
import kotlin.time.Duration.Companion.minutes

/**
 * Repro harness: play many real, decision-heavy games through the [GameSession] recording path, then
 * reconstruct each [CompactReplay] and assert the viewer stream is NOT truncated — i.e. the
 * reconstruction reaches `1 + actions.size` frames with no mid-replay divergence. A failure here is
 * exactly the "only part of the replay was saved" symptom: a recorded action that no longer applies
 * on re-simulation makes [ReplayReconstructor] stop early.
 *
 * This is a heavy fuzzer (dozens of real, decision-heavy games) — far too slow for the PR critical
 * path, so the whole spec is **skipped in CI**. It is opt-in: run it locally with
 * `-DrunReproTests=true` (optionally `-DreproGames=N -DreproSet=EOE,...`) when investigating a
 * replay-truncation regression. The fast, deterministic reconstruction/no-truncation guard that
 * still runs on every PR lives in [CompactReplayReconstructionTest].
 */
class ReplayDivergenceReproTest : ScenarioTestBase() {

    private fun mockWs(id: String): WebSocketSession =
        mockk(relaxed = true) { every { this@mockk.id } returns id }

    private val reproEnabled = System.getProperty("runReproTests")?.toBoolean() == true
    private val numGames = System.getProperty("reproGames")?.toIntOrNull() ?: 5
    private val setCodes = (System.getProperty("reproSet")?.split(",")
        ?: listOf("EOE", "TDM", "DFT", "BLB", "DSK", "MOM"))

    init {
        test("recorded games reconstruct to the full frame stream (no truncation)")
            .config(enabledIf = { reproEnabled }, timeout = 60.minutes) {
            val enumerator = LegalActionEnumerator.create(cardRegistry)
            val reconstructor = ReplayReconstructor(cardRegistry, null)
            val rng = Random(0x5EED)
            val diverged = mutableListOf<String>()

            for (setCode in setCodes) {
              val set = runCatching { MtgSetCatalog.requireByCode(setCode) }.getOrNull() ?: continue
              val nonBasics = set.cards.filter { !it.typeLine.isBasicLand }
              if (nonBasics.size < 30) continue
              for (i in 0 until numGames) {
                val gi = "$setCode-$i"
                val deck1 = buildHeuristicSealedDeck(generateSealedPool(nonBasics, rng))
                val deck2 = buildHeuristicSealedDeck(generateSealedPool(nonBasics, rng))

                val session = GameSession(cardRegistry = cardRegistry, maxPlayers = 2)
                val p1 = EntityId.of("p1-game$gi")
                val p2 = EntityId.of("p2-game$gi")
                session.addPlayer(PlayerSession(mockWs("ws1-$gi"), p1, "Alice"), deck1)
                session.addPlayer(PlayerSession(mockWs("ws2-$gi"), p2, "Bob"), deck2)
                session.startGame()
                session.keepHand(p1)
                session.keepHand(p2)

                playRandomGame(session, enumerator, rng, maxTurns = 50)

                val setup = session.getReplaySetup() ?: continue
                val liveFinal = session.getStateForTesting()
                val actions = session.getRecordedActions()
                val replay = CompactReplay(
                    gameId = session.sessionId,
                    players = session.getPlayers().map { ReplayPlayerInfo(it.playerId.value, it.playerName) },
                    startedAt = Instant.now().toString(),
                    endedAt = Instant.now().toString(),
                    winnerName = null,
                    setup = setup,
                    actions = actions,
                    yields = session.getReplayYields(),
                )

                // Every write to the store — the periodic in-progress flush and the final record
                // alike — serializes the action log via persistenceJson. If any recorded
                // action/response type fails to round-trip, the saved replay is silently short —
                // exactly the "partial replay" symptom.
                val roundTripped = runCatching { ReplayCodec.decode(ReplayCodec.encode(replay)) }
                if (roundTripped.getOrNull() != replay) {
                    val msg = "game $gi: action log does NOT round-trip through the codec " +
                        "(${roundTripped.exceptionOrNull()?.let { it::class.simpleName + ": " + it.message } ?: "value mismatch"})"
                    println("DIVERGED: $msg")
                    diverged.add(msg)
                }

                val reconstructed = reconstructor.reconstruct(replay)
                val expected = 1 + actions.size
                if (reconstructed.frameCount != expected) {
                    val truncIdx = reconstructed.frameCount - 1
                    val msg = "game $gi: reconstructed ${reconstructed.frameCount}/" +
                        "$expected frames (truncated at action $truncIdx = " +
                        "${actions.getOrNull(truncIdx)})"
                    println("DIVERGED: $msg")
                    diverged.add(msg)
                } else if (liveFinal != null) {
                    // No truncation — but did it re-simulate to the SAME state? A silent mismatch
                    // means the saved action log doesn't reproduce what actually happened.
                    val reconFinal = reconstructor.reconstructStateAt(replay, actions.size)
                    if (reconFinal == null || reconFinal.entities != liveFinal.entities ||
                        reconFinal.zones != liveFinal.zones) {
                        val msg = "game $gi: reconstructed final state differs from live (silent divergence)"
                        println("DIVERGED: $msg")
                        diverged.add(msg)
                    }
                }
              }
            }

            if (diverged.isNotEmpty()) {
                throw AssertionError(
                    "${diverged.size} games truncated on reconstruction:\n" +
                        diverged.joinToString("\n")
                )
            }
        }

        // A persistent "always answer" yield is written into GameState and consumed by the pure engine
        // when it auto-answers an optional trigger — but it is NOT a GameAction, so it must be captured
        // separately and re-applied during reconstruction. Without that, the reconstructed game starts
        // with no yields, pauses for a trigger the live game auto-answered, and truncates. This pins the
        // recording + re-application of yields directly.
        test("a persistent yield set mid-game is captured and reproduced on reconstruction")
            .config(enabledIf = { reproEnabled }) {
            val session = GameSession(cardRegistry = cardRegistry, maxPlayers = 2)
            val p1 = EntityId.of("yield-p1")
            val p2 = EntityId.of("yield-p2")
            session.addPlayer(PlayerSession(mockWs("yw1"), p1, "Alice"), mapOf("Forest" to 40))
            session.addPlayer(PlayerSession(mockWs("yw2"), p2, "Bob"), mapOf("Forest" to 40))
            session.startGame()
            session.keepHand(p1)
            session.keepHand(p2)

            fun pass(times: Int) = repeat(times) {
                val s = session.getStateForTesting() ?: return@repeat
                if (s.gameOver) return@repeat
                s.priorityPlayerId?.let { session.executeAutoPass(it) }
            }

            // Play a few turns, then set an "always yes" yield, then play a few more.
            pass(20)
            val identity = com.wingedsheep.sdk.scripting.AbilityIdentity(
                "Some Card#TST-1", com.wingedsheep.sdk.scripting.AbilityId("ability_test_1")
            )
            session.setAbilityYield(p1, identity, com.wingedsheep.engine.state.YieldKind.ALWAYS_ANSWER_YES)
            pass(20)

            val setup = session.getReplaySetup().shouldNotBeNull()
            val recordedYields = session.getReplayYields()
            recordedYields.shouldNotBeEmpty()

            val replay = CompactReplay(
                gameId = session.sessionId,
                players = session.getPlayers().map { ReplayPlayerInfo(it.playerId.value, it.playerName) },
                startedAt = Instant.now().toString(),
                endedAt = Instant.now().toString(),
                winnerName = null,
                setup = setup,
                actions = session.getRecordedActions(),
                yields = recordedYields,
            )

            // Round-trips through the durable codec (carrying the yield entries).
            ReplayCodec.decode(ReplayCodec.encode(replay)) shouldBe replay

            // The reconstructed final state carries the same yields the live game ended with — proof the
            // out-of-band yield was captured and re-applied at the right action position.
            val liveFinal = session.getStateForTesting().shouldNotBeNull()
            val reconstructor = ReplayReconstructor(cardRegistry, null)
            val reconFinal = reconstructor.reconstructStateAt(replay, replay.actions.size).shouldNotBeNull()
            reconFinal.yieldsFor(p1) shouldBe liveFinal.yieldsFor(p1)
            reconFinal.autoAnswerFor(p1, identity) shouldBe true
        }
    }

    private fun playRandomGame(
        session: GameSession,
        enumerator: LegalActionEnumerator,
        rng: Random,
        maxTurns: Int,
    ) {
        var actionCount = 0
        var lastProgressTurn = 0
        var lastProgressAction = 0
        var stuckCount = 0

        while (true) {
            val state = session.getStateForTesting() ?: break
            if (state.gameOver || state.turnNumber >= maxTurns) break

            if (actionCount - lastProgressAction > 1000 && state.turnNumber == lastProgressTurn) {
                stuckCount++
                if (stuckCount >= 3) break
            }
            if (state.turnNumber > lastProgressTurn) {
                lastProgressTurn = state.turnNumber
                lastProgressAction = actionCount
                stuckCount = 0
            }

            val pendingDecision = state.pendingDecision
            val action: GameAction = if (pendingDecision != null) {
                // Right-click "always yes/no" yields are written into GameState but are NOT part of
                // the recorded action stream. The first time we see an optional ability's may-question
                // we register a persistent auto-answer for it; if that ability ever triggers again the
                // engine resolves it silently (no recorded action), so the reconstructed game — which
                // starts with no yields — pauses instead and diverges. This reproduces the production
                // "partial replay" symptom.
                if (pendingDecision is YesNoDecision || pendingDecision is BatchYesNoDecision) {
                    pendingDecision.context.abilityIdentity?.let { id ->
                        session.setAbilityYield(pendingDecision.playerId, id, com.wingedsheep.engine.state.YieldKind.ALWAYS_ANSWER_YES)
                    }
                }
                SubmitDecision(pendingDecision.playerId, randomDecisionResponse(pendingDecision, rng))
            } else {
                val priorityPlayer = state.priorityPlayerId ?: break
                val affordable = enumerator.enumerate(state, priorityPlayer).filter { it.affordable }
                if (affordable.isEmpty()) break
                affordable[rng.nextInt(affordable.size)].action
            }

            val result = session.executeAction(action.playerId, action)
            if (result is GameSession.ActionResult.Failure) {
                val fallback = session.executeAutoPass(action.playerId)
                if (fallback is GameSession.ActionResult.Failure) break
            }
            actionCount++

            // Occasionally exercise undo — the one path that *removes* recorded actions. The
            // priority player after this action attempts to roll back; most attempts fail (no
            // checkpoint), but the ones that succeed truncate the replay log and must keep it
            // replay-consistent with the restored state.
            if (rng.nextInt(5) == 0) {
                val cur = session.getStateForTesting() ?: break
                val pp = cur.priorityPlayerId
                if (pp != null) session.executeUndo(pp)
            }
        }
    }

    private fun randomDecisionResponse(decision: PendingDecision, rng: Random): DecisionResponse {
        return when (decision) {
            is ChooseTargetsDecision -> {
                val targets = decision.targetRequirements.associate { req ->
                    val valid = decision.legalTargets[req.index] ?: emptyList()
                    val count = rng.nextInt(req.minTargets, req.maxTargets + 1).coerceAtMost(valid.size)
                    req.index to valid.shuffled(rng).take(count)
                }
                TargetsResponse(decision.id, targets)
            }

            is SelectCardsDecision -> {
                val count = rng.nextInt(decision.minSelections, decision.maxSelections + 1)
                    .coerceAtMost(decision.options.size)
                CardsSelectedResponse(decision.id, decision.options.shuffled(rng).take(count))
            }

            is YesNoDecision -> YesNoResponse(decision.id, rng.nextBoolean())

            is BatchYesNoDecision -> BatchYesNoResponse(decision.id, choice = rng.nextBoolean(), applyToAll = true)

            is ChooseReplacementDecision -> {
                val fromIndex = rng.nextInt(decision.fromOptions.size)
                val allowed = decision.allowedToByFrom.getOrNull(fromIndex)
                val toIndex = if (allowed != null && allowed.isNotEmpty()) allowed[rng.nextInt(allowed.size)]
                else rng.nextInt(decision.toOptions.size)
                ReplacementChosenResponse(decision.id, fromIndex, toIndex)
            }

            is ChooseModeDecision -> {
                val available = decision.modes.filter { it.available }
                val count = rng.nextInt(decision.minModes, decision.maxModes + 1).coerceAtMost(available.size)
                ModesChosenResponse(decision.id, available.shuffled(rng).take(count).map { it.index })
            }

            is ChooseColorDecision -> {
                val colors = decision.availableColors.toList()
                ColorChosenResponse(decision.id, colors[rng.nextInt(colors.size)])
            }

            is ChooseNumberDecision ->
                NumberChosenResponse(decision.id, rng.nextInt(decision.minValue, decision.maxValue + 1))

            is DistributeDecision -> {
                val dist = mutableMapOf<EntityId, Int>()
                var remaining = decision.totalAmount
                for (target in decision.targets) {
                    dist[target] = decision.minPerTarget
                    remaining -= decision.minPerTarget
                }
                while (remaining > 0 && decision.targets.isNotEmpty()) {
                    val target = decision.targets[rng.nextInt(decision.targets.size)]
                    dist[target] = (dist[target] ?: 0) + 1
                    remaining--
                }
                DistributionResponse(decision.id, dist)
            }

            is OrderObjectsDecision ->
                OrderedResponse(decision.id, decision.objects.shuffled(rng))

            is SplitPilesDecision -> {
                val shuffled = decision.cards.shuffled(rng)
                val splitPoint = if (shuffled.size > 1) rng.nextInt(1, shuffled.size) else 1
                PilesSplitResponse(decision.id, listOf(shuffled.take(splitPoint), shuffled.drop(splitPoint)))
            }

            is ChooseOptionDecision ->
                OptionChosenResponse(decision.id, rng.nextInt(decision.options.size))

            is AssignDamageDecision ->
                DamageAssignmentResponse(decision.id, decision.defaultAssignments)

            is CombatResolutionDecision ->
                CombatResolutionResponse(decision.id, decision.edges.map { DamageEdgeAmount(it.id, it.amount) })

            is BudgetModalDecision -> {
                val selected = mutableListOf<Int>()
                var budget = decision.budget
                val affordable = decision.modes.withIndex().filter { it.value.cost <= budget }
                if (affordable.isNotEmpty()) {
                    val pick = affordable[rng.nextInt(affordable.size)]
                    selected.add(pick.index)
                    budget -= pick.value.cost
                }
                BudgetModalResponse(decision.id, selected)
            }

            is SearchLibraryDecision -> {
                val count = rng.nextInt(decision.minSelections, decision.maxSelections + 1)
                    .coerceAtMost(decision.options.size)
                CardsSelectedResponse(decision.id, decision.options.shuffled(rng).take(count))
            }

            is ReorderLibraryDecision ->
                OrderedResponse(decision.id, decision.cards.shuffled(rng))

            is SelectManaSourcesDecision ->
                ManaSourcesSelectedResponse(decision.id, emptyList(), autoPay = true)
        }
    }

    private fun generateSealedPool(nonBasics: List<CardDefinition>, rng: Random): List<CardDefinition> {
        val commons = nonBasics.filter { it.metadata.rarity == Rarity.COMMON }
        val uncommons = nonBasics.filter { it.metadata.rarity == Rarity.UNCOMMON }
        val rares = nonBasics.filter { it.metadata.rarity == Rarity.RARE }
        val mythics = nonBasics.filter { it.metadata.rarity == Rarity.MYTHIC }

        val pool = mutableListOf<CardDefinition>()
        repeat(6) {
            val usedNames = mutableSetOf<String>()
            fun pick(from: List<CardDefinition>): CardDefinition? {
                val available = from.filter { it.name !in usedNames }
                if (available.isEmpty()) return null
                return available[rng.nextInt(available.size)].also { usedNames.add(it.name) }
            }
            repeat(11) { pick(commons)?.let { pool.add(it) } }
            repeat(3) { pick(uncommons)?.let { pool.add(it) } }
            val rare = if (mythics.isNotEmpty() && rng.nextDouble() < 0.125) pick(mythics) else null
            (rare ?: pick(rares) ?: pick(uncommons) ?: pick(commons))?.let { pool.add(it) }
        }
        return pool
    }
}
