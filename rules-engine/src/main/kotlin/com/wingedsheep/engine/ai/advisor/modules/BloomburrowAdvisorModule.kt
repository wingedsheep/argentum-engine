package com.wingedsheep.engine.ai.advisor.modules

import com.wingedsheep.engine.ai.advisor.*
import com.wingedsheep.engine.ai.evaluation.BoardPresence
import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId

/**
 * AI advisors for Bloomburrow (BLB) cards.
 *
 * Organized by archetype rather than individual cards — many cards share
 * the same strategic pattern (e.g., all combat tricks should be held for combat).
 */
class BloomburrowAdvisorModule : CardAdvisorModule {
    override fun register(registry: CardAdvisorRegistry) {
        registry.register(CombatTrickAdvisor)
        registry.register(InstantRemovalAdvisor)
        registry.register(CounterspellAdvisor)
        registry.register(SpellgyreAdvisor)
        registry.register(BoardWipeAdvisor)
        registry.register(GiftRemovalAdvisor)
        registry.register(GiftBoardWipeAdvisor)
        registry.register(GiftCombatTrickAdvisor)
        registry.register(GiftCounterspellAdvisor)
        registry.register(GiftCardDrawAdvisor)
        registry.register(GiftProtectionAdvisor)
        registry.register(GiftBounceAdvisor)
        registry.register(GiftValueAdvisor)
        registry.register(GraveyardRetrievalAdvisor)
        registry.register(BiteSpellAdvisor)
        registry.register(FlashCreatureAdvisor)
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Shared Utilities
// ═════════════════════════════════════════════════════════════════════════════

/** True when the game is in a combat step where tricks are most effective. */
private fun isCombatStep(state: GameState): Boolean =
    state.step.phase == Phase.COMBAT && state.step != Step.END_COMBAT

/** True when it's the AI's own main phase. */
private fun isOwnMainPhase(state: GameState, playerId: EntityId): Boolean =
    state.activePlayerId == playerId && state.step.isMainPhase

/** True when it's the opponent's turn. */
private fun isOpponentsTurn(state: GameState, playerId: EntityId): Boolean =
    state.activePlayerId != playerId

/** Sum of creature board value for a player. */
private fun creatureBoardValue(state: GameState, projected: ProjectedState, playerId: EntityId): Double {
    return projected.getBattlefieldControlledBy(playerId).sumOf { entityId ->
        val card = state.getEntity(entityId)?.get<CardComponent>() ?: return@sumOf 0.0
        if (projected.isCreature(entityId)) {
            BoardPresence.permanentValue(state, projected, entityId, card)
        } else 0.0
    }
}

/** Count of creatures a player controls. */
private fun creatureCount(projected: ProjectedState, playerId: EntityId): Int =
    projected.getBattlefieldControlledBy(playerId).count { projected.isCreature(it) }

/** Get player's life total. */
private fun lifeTotal(state: GameState, playerId: EntityId): Int =
    state.getEntity(playerId)?.get<LifeTotalComponent>()?.life ?: 0

/** Get player's hand size. */
private fun handSize(state: GameState, playerId: EntityId): Int =
    state.getZone(playerId, Zone.HAND).size

/**
 * Context-sensitive gift penalty. Giving a card to an empty-handed opponent
 * is devastating; giving one to an opponent with 5+ cards barely matters.
 */
private fun giftPenalty(state: GameState, playerId: EntityId): Double {
    val opponentId = state.getOpponent(playerId) ?: return 1.5
    val oppHand = handSize(state, opponentId)
    return when {
        oppHand == 0 -> 4.0   // hellbent opponent — huge cost to give them a card
        oppHand == 1 -> 2.5   // nearly empty — still very costly
        oppHand <= 3 -> 1.5   // moderate hand — standard penalty
        else -> 1.0           // full hand — still a real card
    }
}

/** Simulate both gift modes and pick best, applying context-sensitive gift penalty. */
private fun pickBestGiftMode(context: AdvisorDecisionContext, decision: ChooseModeDecision): DecisionResponse? {
    if (decision.modes.size != 2) return null
    val available = decision.modes.filter { it.available }
    if (available.size <= 1) {
        return ModesChosenResponse(decision.id, listOf(available.first().index))
    }

    val penalty = giftPenalty(context.state, context.playerId)
    val scores = available.map { mode ->
        val result = context.simulator.simulateDecision(
            context.state,
            ModesChosenResponse(decision.id, listOf(mode.index))
        )
        val score = context.evaluator.evaluate(result.state, result.state.projectedState, context.playerId)
        // Apply gift penalty to mode 2 (gift mode is always index 1)
        val adjusted = if (mode.index == 1) score - penalty else score
        mode to adjusted
    }
    val best = scores.maxBy { it.second }
    return ModesChosenResponse(decision.id, listOf(best.first.index))
}

// ═════════════════════════════════════════════════════════════════════════════
// Combat Tricks — hold for combat, don't waste on main phase
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Covers all instant-speed pump spells. The generic AI often casts these during
 * main phase because the 1-ply simulation sees "bigger creature = better board."
 * In practice these should almost always be held for combat.
 */
object CombatTrickAdvisor : CardAdvisor {
    override val cardNames = setOf(
        // Pure pump / protection instants
        "Shore Up",
        "High Stride",
        "Overprotect",
        "Mabel's Mettle",
        "Scales of Shale",
        "Might of the Meek",
        "Rabbit Response",
        "Valley Rally",
        // Flash aura (combat trick)
        "Feather of Flight",
    )

    override fun evaluateCast(context: CastContext): Double? {
        val state = context.state
        val playerId = context.playerId

        // During combat: let default simulation decide (it works well here)
        if (isCombatStep(state)) return null

        // Something on the stack (opponent's removal, combat trick, etc.):
        // let simulation decide freely — it will correctly evaluate whether
        // hexproof/indestructible/toughness buff saves our creature
        if (state.stack.isNotEmpty()) return null

        // Opponent's turn, empty stack: small penalty — hold for combat or
        // a response to removal rather than casting proactively
        if (isOpponentsTurn(state, playerId)) {
            return context.defaultScore - 1.0
        }

        // Own main phase, empty stack: heavily penalize — hold for combat
        if (isOwnMainPhase(state, playerId)) {
            return context.passScore - 1.0
        }

        return null
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Instant Removal — prefer opponent's turn or combat, but allow clearing blockers
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Instant-speed removal should be held for the opponent's turn or combat
 * rather than cast proactively during main phase. Sorcery-speed removal
 * doesn't need an advisor since it can only be cast at sorcery speed anyway.
 */
object InstantRemovalAdvisor : CardAdvisor {
    override val cardNames = setOf(
        "Sonar Strike",
        "Repel Calamity",
        "Take Out the Trash",
        "Early Winter",
        "Conduct Electricity",
    )

    override fun evaluateCast(context: CastContext): Double? {
        val state = context.state
        val playerId = context.playerId

        // During combat or opponent's turn: let simulation decide
        if (isCombatStep(state) || isOpponentsTurn(state, playerId)) return null

        // Own main phase: penalize. The default simulation sees
        // "creature gone = good" but misses the timing advantage
        // of removing an attacker mid-combat.
        if (isOwnMainPhase(state, playerId)) {
            return context.defaultScore - 3.0
        }

        return null
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Counterspells — hold for opponent's turn
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Pure counterspells (no alternate mode). Must be held for opponent's spells.
 */
object CounterspellAdvisor : CardAdvisor {
    override val cardNames = setOf(
        "Dazzling Denial",
    )

    override fun evaluateCast(context: CastContext): Double? {
        val state = context.state
        val playerId = context.playerId

        // Opponent's turn with something on the stack: let simulation decide
        if (isOpponentsTurn(state, playerId) && state.stack.isNotEmpty()) return null

        // Own turn: heavily penalize — hold it
        if (state.activePlayerId == playerId) {
            return context.passScore - 2.0
        }

        // Opponent's turn but nothing to counter: still hold it
        if (state.stack.isEmpty()) {
            return context.passScore - 1.0
        }

        return null
    }
}

/**
 * Spellgyre: modal — counter spell OR surveil 2 + draw 2.
 *
 * Unlike pure counterspells, the draw mode is a fine play on your own turn
 * when you're low on cards. But generally hold for the counter option.
 */
object SpellgyreAdvisor : CardAdvisor {
    override val cardNames = setOf("Spellgyre")

    override fun evaluateCast(context: CastContext): Double? {
        val state = context.state
        val playerId = context.playerId

        // Opponent's turn with something on the stack: always allow (might counter)
        if (isOpponentsTurn(state, playerId) && state.stack.isNotEmpty()) return null

        // Own main phase: only allow when very low on cards (0-1 in hand)
        // and ahead on board — otherwise hold the counter
        if (isOwnMainPhase(state, playerId)) {
            val myHand = handSize(state, playerId)
            if (myHand <= 1) {
                return context.defaultScore - 2.0  // still penalized but not blocked
            }
            return context.passScore - 2.0  // block it — hold the counter
        }

        // Opponent's turn, nothing on stack: hold it
        if (isOpponentsTurn(state, playerId) && state.stack.isEmpty()) {
            return context.passScore - 1.0
        }

        return null
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Board Wipes — only cast when behind on board, consider hand for rebuilding
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Board wipes destroy our own creatures too. Worthwhile when:
 * - Opponent's board is significantly better than ours, OR
 * - We have cards in hand to rebuild and opponent doesn't
 */
object BoardWipeAdvisor : CardAdvisor {
    override val cardNames = setOf(
        "Starfall Invocation",
        "Wildfire Howl",
    )

    override fun evaluateCast(context: CastContext): Double? {
        val state = context.state
        val playerId = context.playerId
        val opponentId = state.getOpponent(playerId) ?: return null
        val projected = context.projected

        val myBoardValue = creatureBoardValue(state, projected, playerId)
        val oppBoardValue = creatureBoardValue(state, projected, opponentId)

        val myHand = handSize(state, playerId)
        val oppHand = handSize(state, opponentId)

        // Hand advantage: having cards to rebuild while opponent is empty
        // shifts the calculus — wiping at parity is fine if we can rebuild
        val handEdge = (myHand - oppHand).coerceIn(-3, 3) * 1.0

        // Ahead on board and no hand advantage: don't wipe
        if (oppBoardValue <= myBoardValue * 1.2 && handEdge <= 0) {
            return context.passScore - 2.0
        }

        // Behind on board: bonus scales with deficit + hand edge
        val deficit = oppBoardValue - myBoardValue
        return context.defaultScore + deficit * 0.3 + handEdge
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Gift Cards — context-sensitive penalty scaling with opponent's hand size
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Gift removal (Nocturnal Hunger, Parting Gust, Consumed by Greed).
 */
object GiftRemovalAdvisor : CardAdvisor {
    override val cardNames = setOf(
        "Nocturnal Hunger",
        "Parting Gust",
        "Consumed by Greed",
    )

    override fun respondToDecision(context: AdvisorDecisionContext): DecisionResponse? {
        val decision = context.decision as? ChooseModeDecision ?: return null
        if (decision.modes.size != 2) return null

        val state = context.state
        val playerId = context.playerId

        // For Parting Gust: permanent exile (gift) is almost always better
        // than temporary exile (base), unless opponent is hellbent
        if (context.sourceCardName == "Parting Gust") {
            val opponentId = state.getOpponent(playerId) ?: return null
            val oppHand = handSize(state, opponentId)
            if (oppHand == 0) {
                // Don't give a hellbent opponent a card for marginal upside
                val available = decision.modes.filter { it.available }
                return ModesChosenResponse(decision.id, listOf(available.first().index))
            }
        }

        // For Nocturnal Hunger: mode 1 = destroy + lose 2 life, mode 2 = gift + destroy (no life loss)
        // Consider both life and opponent hand
        if (context.sourceCardName == "Nocturnal Hunger") {
            val myLife = lifeTotal(state, playerId)
            val opponentId = state.getOpponent(playerId) ?: return null
            val oppHand = handSize(state, opponentId)

            val preferGift = when {
                myLife <= 5 -> true                    // low life — avoid losing 2
                myLife <= 8 && oppHand >= 3 -> true    // medium life, opponent has cards anyway
                oppHand == 0 -> false                  // opponent hellbent — don't gift
                else -> false                          // healthy — take the 2 life hit
            }
            val modeIndex = if (preferGift) 1 else 0
            val available = decision.modes.filter { it.available }
            val chosen = available.getOrNull(modeIndex) ?: available.first()
            return ModesChosenResponse(decision.id, listOf(chosen.index))
        }

        // For others: simulate both with context-sensitive gift penalty
        return pickBestGiftMode(context, decision)
    }
}

/**
 * Gift board wipes (Starfall Invocation, Wildfire Howl).
 *
 * Prefer gift mode when far behind (the extra value outweighs the card),
 * use base mode when the wipe alone is sufficient.
 */
object GiftBoardWipeAdvisor : CardAdvisor {
    override val cardNames = setOf(
        "Starfall Invocation",
        "Wildfire Howl",
    )

    override fun respondToDecision(context: AdvisorDecisionContext): DecisionResponse? {
        val decision = context.decision as? ChooseModeDecision ?: return null
        return pickBestGiftMode(context, decision)
    }
}

/**
 * Gift combat tricks (Crumb and Get It, Valley Rally).
 *
 * Hold for combat (timing), then simulate both modes for the gift decision.
 */
object GiftCombatTrickAdvisor : CardAdvisor {
    override val cardNames = setOf(
        "Crumb and Get It",
        "Valley Rally",
    )

    override fun evaluateCast(context: CastContext): Double? {
        return CombatTrickAdvisor.evaluateCast(context)
    }

    override fun respondToDecision(context: AdvisorDecisionContext): DecisionResponse? {
        val decision = context.decision as? ChooseModeDecision ?: return null
        return pickBestGiftMode(context, decision)
    }
}

/**
 * Gift counterspell (Long River's Pull).
 *
 * Mode 1: counter creature spell only. Mode 2 (gift): counter any spell.
 * For creature spells, prefer non-gift mode unless opponent has full hand.
 */
object GiftCounterspellAdvisor : CardAdvisor {
    override val cardNames = setOf(
        "Long River's Pull",
    )

    override fun evaluateCast(context: CastContext): Double? {
        return CounterspellAdvisor.evaluateCast(context)
    }

    override fun respondToDecision(context: AdvisorDecisionContext): DecisionResponse? {
        val decision = context.decision as? ChooseModeDecision ?: return null
        if (decision.modes.size != 2) return null

        val available = decision.modes.filter { it.available }
        if (available.size <= 1) {
            return ModesChosenResponse(decision.id, listOf(available.first().index))
        }

        // Both modes available means target is a creature spell — prefer non-gift
        val chosen = available.first()
        return ModesChosenResponse(decision.id, listOf(chosen.index))
    }
}

/**
 * Gift card draw (Mind Spiral, Sazacap's Brew).
 *
 * Simulate both modes with context-sensitive gift penalty.
 */
object GiftCardDrawAdvisor : CardAdvisor {
    override val cardNames = setOf(
        "Mind Spiral",
        "Sazacap's Brew",
    )

    override fun respondToDecision(context: AdvisorDecisionContext): DecisionResponse? {
        val decision = context.decision as? ChooseModeDecision ?: return null
        return pickBestGiftMode(context, decision)
    }
}

/**
 * Gift protection spell (Dawn's Truce).
 *
 * Hold for opponent's turn (reactive). Gift mode gives indestructible
 * which is worth a card when protecting significant board presence.
 */
object GiftProtectionAdvisor : CardAdvisor {
    override val cardNames = setOf(
        "Dawn's Truce",
    )

    override fun evaluateCast(context: CastContext): Double? {
        val state = context.state
        val playerId = context.playerId

        if (isOpponentsTurn(state, playerId)) return null

        if (isOwnMainPhase(state, playerId)) {
            return context.passScore - 2.0
        }

        return null
    }

    override fun respondToDecision(context: AdvisorDecisionContext): DecisionResponse? {
        val decision = context.decision as? ChooseModeDecision ?: return null
        return pickBestGiftMode(context, decision)
    }
}

/**
 * Gift bounce spells (Into the Flood Maw).
 *
 * Simulate both modes with context-sensitive gift penalty.
 */
object GiftBounceAdvisor : CardAdvisor {
    override val cardNames = setOf(
        "Into the Flood Maw",
    )

    override fun respondToDecision(context: AdvisorDecisionContext): DecisionResponse? {
        val decision = context.decision as? ChooseModeDecision ?: return null
        return pickBestGiftMode(context, decision)
    }
}

/**
 * Gift value spells — cards where the gift mode gives strictly more value
 * (extra targets, extra cards returned) and the decision is purely about
 * whether the upgrade is worth giving the opponent a card.
 *
 * Covers: Blooming Blast, Coiling Rebirth, Dewdrop Cure, Peerless Recycling,
 * Wear Down, Cruelclaw's Heist.
 */
object GiftValueAdvisor : CardAdvisor {
    override val cardNames = setOf(
        "Blooming Blast",
        "Coiling Rebirth",
        "Dewdrop Cure",
        "Peerless Recycling",
        "Wear Down",
        "Cruelclaw's Heist",
    )

    override fun respondToDecision(context: AdvisorDecisionContext): DecisionResponse? {
        val decision = context.decision as? ChooseModeDecision ?: return null
        return pickBestGiftMode(context, decision)
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Graveyard Retrieval — always return creatures when possible
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Cards that return creatures from graveyard to hand (Hazel's Nocturne, etc.).
 *
 * The generic AI sometimes chooses 0 targets on "up to N" graveyard retrieval
 * because the 1-ply simulation doesn't fully value cards in hand. Getting
 * creatures back is almost always correct — override to select the maximum.
 */
object GraveyardRetrievalAdvisor : CardAdvisor {
    override val cardNames = setOf(
        "Hazel's Nocturne",
    )

    override fun respondToDecision(context: AdvisorDecisionContext): DecisionResponse? {
        val decision = context.decision as? ChooseTargetsDecision ?: return null
        if (decision.targetRequirements.size != 1) return null

        val req = decision.targetRequirements.first()
        val targets = decision.legalTargets[req.index] ?: return null
        if (targets.isEmpty()) return null

        // Select up to maxTargets creatures, preferring higher mana value
        val state = context.state
        val ranked = targets.sortedByDescending { entityId ->
            val card = state.getEntity(entityId)?.get<CardComponent>()
            card?.manaValue ?: 0
        }

        val count = req.maxTargets.coerceAtMost(ranked.size)
        return TargetsResponse(decision.id, mapOf(req.index to ranked.take(count)))
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Flash Creatures — prefer casting on opponent's turn after attackers declared
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Flash creatures with big bodies (e.g., Galewind Moose 6/6 flash vigilance reach).
 *
 * The generic AI casts these during its own main phase because it sees
 * "big creature = good board." But flash creatures are far more valuable
 * when cast after the opponent declares attackers — they can block
 * immediately and the opponent can't play around them.
 */
object FlashCreatureAdvisor : CardAdvisor {
    override val cardNames = setOf(
        "Galewind Moose",
    )

    override fun evaluateCast(context: CastContext): Double? {
        val state = context.state
        val playerId = context.playerId

        // Opponent's declare attackers or later combat step: ideal timing — ambush block
        if (isOpponentsTurn(state, playerId) && isCombatStep(state)) {
            return context.defaultScore + 3.0
        }

        // Opponent's turn outside combat: still good (they can't remove it before attacking)
        if (isOpponentsTurn(state, playerId)) return null

        // Own main phase: penalize — hold for opponent's combat
        if (isOwnMainPhase(state, playerId)) {
            return context.defaultScore - 4.0
        }

        return null
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Bite / Fight spells — joint target simulation
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Bite and fight spells — cards where your creature deals damage to (or fights)
 * an opponent's creature. The two targets are coupled — damage depends on the
 * attacking creature's power — but the generic AI picks each target independently.
 *
 * This advisor does joint simulation: try combinations of (my creature, their creature)
 * and pick the pair that produces the best board state. This lets the AI understand
 * that pumping the 4/4 to deal 5 damage kills the opponent's 5/5, or that the 6/6
 * should fight the 5/5 rather than the 1/1.
 *
 * Covers: Rabid Gnaw (bite + pump), Polliwallop (bite × 2), Longstalk Brawl (fight),
 * Hunter's Talent (bite on ETB).
 */
object BiteSpellAdvisor : CardAdvisor {
    override val cardNames = setOf(
        "Rabid Gnaw",
        "Polliwallop",
        "Longstalk Brawl",
        "Hunter's Talent",
    )

    override fun respondToDecision(context: AdvisorDecisionContext): DecisionResponse? {
        // Handle gift mode choice for Longstalk Brawl
        if (context.decision is ChooseModeDecision) {
            return pickBestGiftMode(context, context.decision)
        }

        val decision = context.decision as? ChooseTargetsDecision ?: return null
        if (decision.targetRequirements.size != 2) return null

        val req0 = decision.targetRequirements[0]
        val req1 = decision.targetRequirements[1]
        val myCreatures = decision.legalTargets[req0.index] ?: return null
        val theirCreatures = decision.legalTargets[req1.index] ?: return null

        if (myCreatures.isEmpty() || theirCreatures.isEmpty()) return null

        // Cap combinations to avoid explosion (6 * 6 = 36 simulations max)
        val myCandidates = myCreatures.take(6)
        val theirCandidates = theirCreatures.take(6)

        var bestScore = Double.NEGATIVE_INFINITY
        var bestMine: EntityId = myCandidates.first()
        var bestTheirs: EntityId = theirCandidates.first()

        for (mine in myCandidates) {
            for (theirs in theirCandidates) {
                val response = TargetsResponse(
                    decision.id,
                    mapOf(req0.index to listOf(mine), req1.index to listOf(theirs))
                )
                val result = context.simulator.simulateDecision(context.state, response)
                val score = context.evaluator.evaluate(
                    result.state, result.state.projectedState, context.playerId
                )
                if (score > bestScore) {
                    bestScore = score
                    bestMine = mine
                    bestTheirs = theirs
                }
            }
        }

        return TargetsResponse(
            decision.id,
            mapOf(req0.index to listOf(bestMine), req1.index to listOf(bestTheirs))
        )
    }
}
