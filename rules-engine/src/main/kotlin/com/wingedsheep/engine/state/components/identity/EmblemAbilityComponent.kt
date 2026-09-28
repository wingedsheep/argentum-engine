package com.wingedsheep.engine.state.components.identity

import com.wingedsheep.engine.state.Component
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.StaticAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import kotlinx.serialization.Serializable

/** Activated abilities granted dynamically by a permanent emblem. */
@Serializable
data class EmblemActivatedAbilityComponent(
    val filter: GroupFilter,
    val abilities: List<ActivatedAbility>,
) : Component

/**
 * Static abilities the emblem *itself* has, as though printed on it — for emblem text that reads on
 * its controller rather than on a group of permanents ("You may cast spells from your hand without
 * paying their mana costs", Tamiyo, Field Researcher's −7).
 *
 * The synthetic emblem entity is never registered in a zone, so scans that walk the battlefield
 * looking for a printed static won't see it; a scan that should honor an emblem consults this
 * component alongside the battlefield (see
 * [com.wingedsheep.engine.mechanics.mana.CostCalculator.hasFreeCastPermission]).
 */
@Serializable
data class EmblemStaticAbilityComponent(
    val abilities: List<StaticAbility>,
) : Component

/**
 * Every static ability on an emblem [playerId] controls, paired with the emblem entity that has it
 * — the emblem half of a "does this player have permission X" scan. An emblem is always controlled
 * by the player who got it, so its statics read exactly like those of a permanent that player
 * controls (Wrenn and Realmbreaker's "You may play lands and cast permanent spells from your
 * graveyard" is [com.wingedsheep.sdk.scripting.MayPlayLandsFromGraveyard] plus
 * [com.wingedsheep.sdk.scripting.MayCastFromGraveyard], the statics Crucible of Worlds and its kin
 * print). The emblem id stands in for the source permanent wherever a scan keys per-source state
 * off it, such as a `oncePerTurn` marker.
 */
fun GameState.emblemStaticAbilitiesOf(
    playerId: EntityId
): List<Pair<EntityId, StaticAbility>> {
    val result = mutableListOf<Pair<EntityId, StaticAbility>>()
    for ((entityId, container) in entities) {
        val statics = container.get<EmblemStaticAbilityComponent>() ?: continue
        if (container.get<ControllerComponent>()?.playerId != playerId) continue
        for (ability in statics.abilities) result.add(entityId to ability)
    }
    return result
}
