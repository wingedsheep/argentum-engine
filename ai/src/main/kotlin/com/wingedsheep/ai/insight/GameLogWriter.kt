package com.wingedsheep.ai.insight

import com.wingedsheep.engine.core.GameAction
import com.wingedsheep.engine.core.PendingDecision
import com.wingedsheep.engine.legalactions.EnumerationMode
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ActivatedAbilityOnStackComponent
import com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId

/**
 * Renders a game as the human-readable log analysts read for AI misplays: every seat's board and
 * hand each turn, every decision and its answer, every action taken, and what a player had on offer
 * when it passed. Pure rendering — the caller drives the game (live AI self-play in
 * `GameLogBenchmark`, a re-simulated server replay in game-server's `ReplayGameLogRenderer`) and
 * reports each step here.
 *
 * Entity ids in action/decision text (`e123`) are replaced by card names, with P/T for creatures on
 * the battlefield; seat ids by [seatLabels]. Default-valued fields of the action data classes are
 * dropped so a line shows only what was chosen.
 */
class GameLogWriter(
    private val seatIds: List<EntityId>,
    private val enumerator: LegalActionEnumerator,
    seatLabels: Map<EntityId, String> = seatIds.withIndex().associate { (i, id) -> id to "Seat$i" },
    val out: StringBuilder = StringBuilder(),
) {
    private val labels = seatLabels
    private var lastTurn = -1
    private val seatPattern = Regex(
        labels.keys.joinToString("|", "(?<![\\w-])(?:", ")(?![\\w-])") { Regex.escape(it.value) }
    )

    fun label(playerId: EntityId): String = labels[playerId] ?: "?${playerId.value}"

    fun line(text: String = "") {
        out.appendLine(text)
    }

    /** Emits the turn header and both boards the first time [state] shows a new turn number. */
    fun turnIfNew(state: GameState) {
        if (state.turnNumber == lastTurn) return
        lastTurn = state.turnNumber
        line("\n=== TURN ${state.turnNumber} — active ${state.activePlayerId?.let(::label) ?: "-"} ===")
        board(state)
    }

    fun decision(state: GameState, decision: PendingDecision, response: Any) {
        line("  [${state.step}] ${label(decision.playerId)} DECISION ${decision::class.simpleName}: " +
            humanize(state, dropField(decision.toString(), "cardInfo")).take(600))
        line("      -> ${humanize(state, response.toString()).take(300)}")
    }

    fun action(state: GameState, playerId: EntityId, action: GameAction) {
        val stackTop = state.stack.lastOrNull()?.let { " (stack top: ${nameOf(state, it)})" }.orEmpty()
        line("  [${state.step}] ${label(playerId)}: ${humanize(state, action.toString()).take(500)}$stackTop")
    }

    /**
     * The affordable, non-mana actions [playerId] could take in [state] — what was on offer at a
     * priority point. Mana abilities are left out; they are never a play on their own.
     */
    fun offered(state: GameState, playerId: EntityId): List<String> =
        enumerator.enumerate(state, playerId, EnumerationMode.ACTIONS_ONLY)
            .filter {
                it.affordable && it.actionType != "PassPriority" && !it.isManaAbility && !it.description.contains(": Add {")
            }
            .map { humanize(state, it.description) }
            .distinct()

    /** A pass that left [offered] plays unused — the most common "did nothing" misplay. */
    fun passWithOffers(state: GameState, playerId: EntityId, offered: List<String> = offered(state, playerId)) {
        if (offered.isEmpty()) return
        val stackTop = state.stack.lastOrNull()?.let { " (stack top: ${nameOf(state, it)})" }.orEmpty()
        line("  [${state.step}] ${label(playerId)} passes$stackTop; had: ${offered.joinToString(" | ").take(500)}")
    }

    /** The legal alternatives to an action [playerId] took, on the line under it. */
    fun options(offered: List<String>) {
        if (offered.size > 1) line("      options: ${offered.joinToString(" | ").take(600)}")
    }

    fun board(state: GameState) {
        seatIds.forEach { pid ->
            val bf = state.getBattlefield(pid)
            val lands = bf.filter { state.getEntity(it)?.get<CardComponent>()?.typeLine?.isLand == true }
            val others = bf - lands.toSet()
            val hand = state.getZone(pid, Zone.HAND).mapNotNull { state.getEntity(it)?.get<CardComponent>() }
            line("    ${label(pid)} life=${state.lifeTotal(pid)} lib=${state.getZone(pid, Zone.LIBRARY).size} " +
                "gy=${state.getZone(pid, Zone.GRAVEYARD).size} " +
                "lands=${lands.size}(${lands.count { state.getEntity(it)?.has<TappedComponent>() == true }}T)")
            line("      board: ${others.joinToString("; ") { perm(state, it) }}")
            line("      hand: ${hand.joinToString("; ") { "${it.name} ${it.manaCost}" }}")
        }
    }

    fun nameOf(state: GameState, id: EntityId): String {
        labels[id]?.let { return it }
        val e = state.getEntity(id) ?: return "?$id"
        e.get<TriggeredAbilityOnStackComponent>()?.let { return "${it.sourceName} trigger" }
        e.get<ActivatedAbilityOnStackComponent>()?.let { return "${it.sourceName} ability" }
        val card = e.get<CardComponent>() ?: return "obj"
        val p = state.projectedState
        return if (p.isCreature(id) && id in state.getBattlefield()) "${card.name}(${p.getPower(id)}/${p.getToughness(id)})" else card.name
    }

    fun humanize(state: GameState, text: String): String {
        // The acting player is already the line's label; other seat ids (attack targets, chosen
        // players) read as their label — server seat ids are UUIDs, not `eN`, so they get their own pass.
        // A chosen player target's `playerId=` is the choice itself, so it is unwrapped before the strip.
        val unwrapped = PLAYER_TARGET.replace(text, "Player(")
        val t = PLAYER_FIELD.replace(NOISE.replace(NOISE.replace(unwrapped, ""), ""), "")
        return ENTITY.replace(seatPattern.replace(t) { m -> labels.getValue(EntityId(m.value)) }) { m ->
            nameOf(state, EntityId(m.value))
        }
    }

    /**
     * Removes `, <name>=<value>` from a data-class `toString`, the value's brackets balanced — for
     * per-option card details that repeat what the option names already say.
     */
    private fun dropField(text: String, name: String): String {
        val start = text.indexOf(", $name=")
        if (start < 0) return text
        var depth = 0
        var i = start + name.length + 3
        while (i < text.length) {
            when (text[i]) {
                '(', '[', '{' -> depth++
                ')', ']', '}' -> if (depth == 0) break else depth--
                ',' -> if (depth == 0) break
            }
            i++
        }
        return text.removeRange(start, i)
    }

    private fun perm(state: GameState, id: EntityId): String {
        val e = state.getEntity(id) ?: return "?$id"
        val card = e.get<CardComponent>() ?: return "obj"
        val p = state.projectedState
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

    private companion object {
        val ENTITY = Regex("\\be\\d+\\b")

        /** A `Player(playerId=…)` target or attack target, rendered as `Player(<seat>)`. */
        val PLAYER_TARGET = Regex("\\bPlayer\\(playerId=")

        /** The `playerId=` field every action/decision opens with. */
        val PLAYER_FIELD = Regex("(?<=\\()playerId=[^,)]+(?:, )?|, playerId=[^,)]+")

        /** Default-valued fields of the action data classes; run twice so adjacent ones both go. */
        val NOISE = Regex(", \\w+=(?:null|false|\\[]|\\{}|0|1|AutoPay)(?=[,)])")
    }
}
