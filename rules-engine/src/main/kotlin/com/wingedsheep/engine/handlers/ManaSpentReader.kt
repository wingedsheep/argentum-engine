package com.wingedsheep.engine.handlers

import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.CastRecordComponent
import com.wingedsheep.engine.state.components.stack.SpellOnStackComponent
import com.wingedsheep.sdk.model.EntityId

/**
 * Single source of truth for reading the mana actually spent to cast an entity.
 *
 * Reads the live [SpellOnStackComponent] buckets while the entity is still a spell on the
 * stack, otherwise the [CastRecordComponent] snapshot stamped when it resolved onto the
 * battlefield. Both record the full payment broken down by color — for `{X}` spells the X
 * portion is already folded into the per-color buckets by the mana solver, so these counts
 * include mana spent on X. All-zero when neither component is present (the entity was put
 * onto the battlefield without being cast, or is a copy created on the stack — no mana was
 * spent in either case), which is CR-faithful for "the mana spent to cast it" payoffs.
 *
 * Shared by [PredicateEvaluator] (mana-value-vs-mana-spent predicates) and
 * [DynamicAmountEvaluator] (Converge / total-mana-spent amounts) so the read path can never
 * drift between the two.
 */
object ManaSpentReader {

    /** The five colored buckets (W, U, B, R, G) spent to cast [entityId]; colorless excluded. */
    private fun coloredBuckets(state: GameState, entityId: EntityId): IntArray {
        val container = state.getEntity(entityId) ?: return IntArray(5)
        container.get<SpellOnStackComponent>()?.let {
            return intArrayOf(it.manaSpentWhite, it.manaSpentBlue, it.manaSpentBlack, it.manaSpentRed, it.manaSpentGreen)
        }
        container.get<CastRecordComponent>()?.let {
            return intArrayOf(it.whiteSpent, it.blueSpent, it.blackSpent, it.redSpent, it.greenSpent)
        }
        return IntArray(5)
    }

    /** Total mana (all colors plus colorless) spent to cast [entityId]; 0 if it wasn't cast. */
    fun totalSpent(state: GameState, entityId: EntityId): Int {
        val container = state.getEntity(entityId) ?: return 0
        container.get<SpellOnStackComponent>()?.let {
            return it.manaSpentWhite + it.manaSpentBlue + it.manaSpentBlack +
                it.manaSpentRed + it.manaSpentGreen + it.manaSpentColorless
        }
        container.get<CastRecordComponent>()?.let {
            return it.whiteSpent + it.blueSpent + it.blackSpent +
                it.redSpent + it.greenSpent + it.colorlessSpent
        }
        return 0
    }

    /**
     * The number of distinct *colors* of mana spent to cast [entityId] (0–5). Colorless is not
     * a color (CR 105.1), so it never contributes. Backs the Converge ability word and Sunburst.
     */
    fun distinctColorsSpent(state: GameState, entityId: EntityId): Int =
        coloredBuckets(state, entityId).count { it > 0 }
}
