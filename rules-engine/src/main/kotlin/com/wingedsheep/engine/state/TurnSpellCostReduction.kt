package com.wingedsheep.engine.state

import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import kotlinx.serialization.Serializable

/**
 * A turn-scoped "spells you cast this turn that match [spellFilter] cost {[amount]} less to cast"
 * discount, installed by
 * [com.wingedsheep.sdk.scripting.effects.ReduceSpellCostsThisTurnEffect] (Will, Scion of Peace;
 * Rowan, Scion of War).
 *
 * The repeating counterpart of [PendingNextSpellAffinity]: that rider is consumed by the first
 * matching spell, this one keeps applying until the turn ends. Two differences drive the shape:
 *
 * - **[amount] is already resolved.** The Scion rulings fix X at the moment the activated ability
 *   resolves, so the executor evaluates the [com.wingedsheep.sdk.scripting.values.DynamicAmount]
 *   once and stores the number. Re-reading it per cast would let life gained after activation
 *   inflate the discount.
 * - **It lives on the game state, not the source.** The ability has already resolved, so the
 *   discount must outlive the source leaving the battlefield.
 *
 * Cleared at every turn boundary by [com.wingedsheep.engine.core.TurnManager.startTurn].
 *
 * @property controllerId The player whose matching spells are discounted.
 * @property spellFilter Which of that player's spells the discount applies to.
 * @property amount Generic mana taken off each matching spell (CR 601.2f — colored mana is never
 *   reduced, and the mana component is floored at {0}).
 * @property sourceId The entity that created this discount.
 * @property sourceName Human-readable name of the source.
 */
@Serializable
data class TurnSpellCostReduction(
    val controllerId: EntityId,
    val spellFilter: GameObjectFilter,
    val amount: Int,
    val sourceId: EntityId,
    val sourceName: String,
)
