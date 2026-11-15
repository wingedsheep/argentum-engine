package com.wingedsheep.engine.state

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.AfterResolveDestination
import kotlinx.serialization.Serializable

/**
 * Tracks a pending "the next time you cast a [spellFilter] spell this turn, put that card into
 * [destination] instead of into your graveyard as it resolves" rider, installed by
 * [com.wingedsheep.sdk.scripting.effects.GrantNextSpellResolveDestinationEffect] (Soulfire Grand
 * Master).
 *
 * Sibling of [PendingUncounterableSpell]: when [controllerId] next casts a matching spell from one of
 * [fromZones], `CastTriggers.consumeResolveDestinationRiders` consumes every matching entry and stamps that spell with an
 * [com.wingedsheep.engine.state.components.identity.AfterResolveDestinationComponent] that applies
 * only if it resolves. Cleared at every turn boundary by
 * [com.wingedsheep.engine.core.TurnManager.startTurn].
 *
 * @property controllerId The player whose next matching spell is redirected.
 * @property destination Where the card goes instead of its owner's graveyard as it resolves.
 * @property spellFilter Which spell the rider waits for.
 * @property fromZones The zones a covered spell must be cast from, or `null` for any zone.
 * @property sourceId The entity that created this rider (source-relative filter context).
 * @property sourceName Human-readable name of the source, shown on the client badge.
 */
@Serializable
data class PendingNextSpellResolveDestination(
    val controllerId: EntityId,
    val destination: AfterResolveDestination,
    val spellFilter: GameObjectFilter,
    val fromZones: Set<Zone>? = null,
    val sourceId: EntityId,
    val sourceName: String
) {
    /** Whether a cast from [zone] (`null` means hand) is covered — same reading as [PendingFreeCastSpell.allowsCastFrom]. */
    fun allowsCastFrom(zone: Zone?): Boolean = fromZones == null || (zone ?: Zone.HAND) in fromZones
}
