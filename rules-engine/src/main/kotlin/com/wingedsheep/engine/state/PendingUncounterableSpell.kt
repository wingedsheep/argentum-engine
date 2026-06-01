package com.wingedsheep.engine.state

import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import kotlinx.serialization.Serializable

/**
 * Tracks a pending "next spell can't be countered" rider (e.g., Mistrise Village).
 *
 * When [controllerId] next casts a spell matching [spellFilter] this turn, that spell is
 * stamped uncounterable (a `CantBeCounteredComponent`) and this entry is consumed (removed).
 *
 * One-shot counterpart to the duration-based player grant
 * [com.wingedsheep.engine.state.components.player.SpellsCantBeCounteredComponent]: that one
 * protects *every* matching spell cast for a whole duration; this protects only the next one.
 * Structurally it mirrors [PendingSpellCopy] ("copy your next spell").
 *
 * @property controllerId The player whose next matching spell becomes uncounterable.
 * @property spellFilter Which spell the rider waits for (defaults to any spell).
 * @property sourceId The entity that created this rider (e.g., Mistrise Village).
 * @property sourceName Human-readable name of the source.
 */
@Serializable
data class PendingUncounterableSpell(
    val controllerId: EntityId,
    val spellFilter: GameObjectFilter = GameObjectFilter.Any,
    val sourceId: EntityId,
    val sourceName: String
)
