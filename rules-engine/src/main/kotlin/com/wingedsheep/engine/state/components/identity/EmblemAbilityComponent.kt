package com.wingedsheep.engine.state.components.identity

import com.wingedsheep.engine.state.Component
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
