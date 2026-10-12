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
 * Activated abilities the emblem *itself* has ("Tap an untapped artifact you control: This emblem
 * deals 1 damage to any target." — Karn, Living Legacy's −7). The emblem is the ability's source:
 * its controller activates it from the command zone (CR 114.4), and it is the object that deals the
 * damage. Contrast [EmblemActivatedAbilityComponent], which hands abilities to a group of
 * permanents.
 *
 * An emblem carrying this also carries a synthetic [CardComponent] with no types, colors or mana
 * cost (CR 114.3), so the activation pipeline — which names, prices and validates its source
 * through one — reads the emblem as it reads a permanent. See [emblemsWithOwnedActivatedAbilities].
 */
@Serializable
data class EmblemOwnedActivatedAbilityComponent(
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

/**
 * Every emblem [playerId] controls that has activated abilities of its own — the extra "sources"
 * the activation enumerator walks beside the battlefield. Emblems are owned and controlled by the
 * player who got them (CR 114.2), and never change hands.
 */
fun GameState.emblemsWithOwnedActivatedAbilities(playerId: EntityId): List<EntityId> {
    val result = mutableListOf<EntityId>()
    for ((entityId, container) in entities) {
        if (!container.has<EmblemOwnedActivatedAbilityComponent>()) continue
        if (container.get<ControllerComponent>()?.playerId != playerId) continue
        result.add(entityId)
    }
    return result
}
