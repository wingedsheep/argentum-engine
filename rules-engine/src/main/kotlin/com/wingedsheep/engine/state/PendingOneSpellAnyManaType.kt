package com.wingedsheep.engine.state

import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import kotlinx.serialization.Serializable

/**
 * Tracks a pending "for one spell this turn, you may spend mana as though it were mana of any type
 * to pay that spell's mana cost" rider (North Star), installed by
 * [com.wingedsheep.sdk.scripting.effects.GrantOneSpellAnyManaTypeEffect].
 *
 * Unlike the "next spell" riders ([PendingNextSpellKeyword], [PendingFreeCastSpell]) a cast doesn't
 * spend it by matching: [com.wingedsheep.engine.handlers.actions.spell.CastCostPayer] applies it —
 * and removes it — only for a [controllerId] spell matching [spellFilter] whose mana cost can't be
 * paid without it. The cast enumerators read it as a may-spend-any-type grant, so a spell it alone
 * makes affordable is offered.
 *
 * Cleared at every turn boundary by [com.wingedsheep.engine.core.TurnManager.startTurn].
 *
 * @property controllerId The player who may spend mana of any type on one spell.
 * @property spellFilter Which spells the rider may apply to.
 * @property sourceId The entity that created this rider.
 * @property sourceName Human-readable name of the source, shown on the client badge.
 */
@Serializable
data class PendingOneSpellAnyManaType(
    val controllerId: EntityId,
    val spellFilter: GameObjectFilter,
    val sourceId: EntityId,
    val sourceName: String
)
