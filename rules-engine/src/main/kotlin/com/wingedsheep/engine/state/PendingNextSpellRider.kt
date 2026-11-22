package com.wingedsheep.engine.state

import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ManaSpellRider
import kotlinx.serialization.Serializable

/**
 * Tracks a pending "the next spell you cast this turn gets [rider]" grant
 * ([com.wingedsheep.sdk.scripting.effects.GrantNextSpellRiderEffect] — Mistrise Village's
 * "can't be countered", Savage Summoning's additional +1/+1 counter).
 *
 * When [controllerId] next casts a spell matching [spellFilter] this turn, [rider] is applied to
 * that spell exactly as if mana carrying it had paid for the spell, and this entry is consumed
 * (removed). Structurally it mirrors [PendingSpellCopy] ("copy your next spell").
 *
 * @property controllerId The player whose next matching spell gets the rider.
 * @property spellFilter Which spell the rider waits for (defaults to any spell).
 * @property rider What that spell gets.
 * @property sourceId The entity that created this rider (e.g., Mistrise Village).
 * @property sourceName Human-readable name of the source.
 */
@Serializable
data class PendingNextSpellRider(
    val controllerId: EntityId,
    val spellFilter: GameObjectFilter = GameObjectFilter.Any,
    val rider: ManaSpellRider = ManaSpellRider.MakesSpellUncounterable(),
    val sourceId: EntityId,
    val sourceName: String
)
