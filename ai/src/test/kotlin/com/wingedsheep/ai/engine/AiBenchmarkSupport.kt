package com.wingedsheep.ai.engine

import com.wingedsheep.engine.core.DeclareAttackers
import com.wingedsheep.engine.core.DeclareBlockers
import com.wingedsheep.engine.core.GameAction
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.legalactions.EnumerationMode
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.combat.AttackersDeclaredThisCombatComponent
import com.wingedsheep.engine.state.components.combat.BlockersDeclaredThisCombatComponent
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.model.Rarity
import kotlin.random.Random

/**
 * Shared helpers for the benchmarks that drive real AI-vs-AI games —
 * [SimulationThroughputBenchmark] and the arena (`com.wingedsheep.ai.arena`).
 *
 * The unseeded deck builders in [GameBenchmark] and [AdvisorBenchmark] predate this and are left
 * alone: they measure per-game cost, where deck reproducibility buys nothing.
 */

// ─────────────────────────────────────────────────────────────────────────────
// Seeded sealed decks — same seed, same decks, every rerun
// ─────────────────────────────────────────────────────────────────────────────

/** Opens six boosters' worth of cards and autobuilds a 40-card limited deck from them. */
internal fun buildSeededSealedDeck(allCards: List<CardDefinition>, rng: Random): Deck {
    val pool = generateSeededSealedPool(allCards, rng)
    val deckMap = buildHeuristicSealedDeck(pool)
    return Deck(deckMap.flatMap { (name, count) -> List(count) { name } })
}

/** Six boosters: 11 commons, 3 uncommons and a rare (1-in-8 mythic) each, no duplicates per pack. */
internal fun generateSeededSealedPool(allCards: List<CardDefinition>, rng: Random): List<CardDefinition> {
    val nonBasics = allCards.filter { !it.typeLine.isBasicLand }
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
        pool.add(rare ?: pick(rares) ?: pick(uncommons) ?: pick(commons)!!)
    }
    return pool
}

// ─────────────────────────────────────────────────────────────────────────────
// Illegal-action recovery
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Mirror of `AIPlayer.playPriorityWindow`'s fallback: when the chosen action is rejected, pass —
 * except in a combat declaration step, where passing leaves mandatory attackers/blockers
 * undeclared and the game wedges.
 *
 * A benchmark that needs this has already found a bug; the point is to keep the *rest* of the run
 * going so the exception histogram sees every distinct failure, not just the first.
 */
internal fun safeFallbackAction(
    state: GameState,
    playerId: EntityId,
    enumerator: LegalActionEnumerator
): GameAction {
    val attackersDeclared = state.getEntity(playerId)?.has<AttackersDeclaredThisCombatComponent>() == true
    val blockersDeclared = state.getEntity(playerId)?.has<BlockersDeclaredThisCombatComponent>() == true
    return when {
        state.step == Step.DECLARE_ATTACKERS && state.activePlayerId == playerId && !attackersDeclared -> {
            val la = enumerator.enumerate(state, playerId, EnumerationMode.ACTIONS_ONLY)
                .find { it.actionType == "DeclareAttackers" }
            val mandatory = la?.mandatoryAttackers ?: emptyList()
            val opponentId = state.getOpponents(playerId).firstOrNull()
            DeclareAttackers(
                playerId,
                if (mandatory.isNotEmpty() && opponentId != null) mandatory.associateWith { opponentId } else emptyMap()
            )
        }

        state.step == Step.DECLARE_BLOCKERS && state.activePlayerId != playerId && !blockersDeclared -> {
            val la = enumerator.enumerate(state, playerId, EnumerationMode.ACTIONS_ONLY)
                .find { it.actionType == "DeclareBlockers" }
            val mandatory = la?.mandatoryBlockerAssignments ?: emptyMap()
            DeclareBlockers(playerId, mandatory.mapValues { (_, targets) -> targets.take(1) })
        }

        else -> PassPriority(playerId)
    }
}
