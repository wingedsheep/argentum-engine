package com.wingedsheep.engine.state

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import kotlinx.serialization.Serializable

/**
 * Tracks a pending "[spellFilter] spells you cast this turn can be cast without paying their mana
 * costs" permission, installed by [com.wingedsheep.sdk.scripting.effects.GrantFreeCastThisTurnEffect]:
 * the one-shot "the next … spell" rider (World War Hulk chapter I, [nextSpellOnly]) or the
 * whole-turn "until end of turn, you may cast spells from your hand …" (Chandra, Flame's Catalyst).
 *
 * Sibling of [PendingUncounterableSpell] and [PendingNextSpellAffinity]:
 * [com.wingedsheep.engine.mechanics.mana.CostCalculator.hasFreeCastPermission] reads it so the
 * `CastSpell.useWithoutPayingManaCost` variant is offered for a matching card, and
 * [com.wingedsheep.engine.handlers.actions.spell.CastSpellHandler] removes a [nextSpellOnly] one once
 * [controllerId] casts a matching spell; a whole-turn one is never consumed.
 *
 * **A one-shot rider is consumed by the cast, not by the free cast.** "The next … spell you cast this turn" names a
 * spell; casting a matching spell for full price makes *that* spell the one the effect applied to,
 * so the rider is spent either way. Same contract as the affinity rider, which is likewise
 * consumed by a matching cast whose cost the affinity never actually changed.
 *
 * Cleared at every turn boundary by [com.wingedsheep.engine.core.TurnManager.startTurn].
 *
 * @property controllerId The player whose matching spells may be cast for free.
 * @property spellFilter Which spell the rider waits for (World War Hulk: red or green creature).
 * @property sourceId The entity that created this rider — put into the
 *   [com.wingedsheep.engine.handlers.PredicateContext] so source-relative predicates in
 *   [spellFilter] resolve against it.
 * @property sourceName Human-readable name of the source, shown on the client badge.
 * @property nextSpellOnly Whether the first matching cast consumes it.
 * @property fromZones The zones a covered spell must be cast from, or `null` for any zone.
 */
@Serializable
data class PendingFreeCastSpell(
    val controllerId: EntityId,
    val spellFilter: GameObjectFilter,
    val sourceId: EntityId,
    val sourceName: String,
    val nextSpellOnly: Boolean = true,
    val fromZones: Set<Zone>? = null
) {
    /** Whether a cast from [zone] (`null` means hand) is covered — same reading as `MayCastWithoutPayingManaCost.allowsCastFrom`. */
    fun allowsCastFrom(zone: Zone?): Boolean = fromZones == null || (zone ?: Zone.HAND) in fromZones
}
