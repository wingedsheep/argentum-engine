package com.wingedsheep.mtg.sets.definitions.legions.cards

import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantTriggeredAbilityToCreatureGroup
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.effects.GainLifeEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Essence Sliver
 * {3}{W}
 * Creature — Sliver
 * 3/3
 * Whenever a Sliver deals damage, its controller gains that much life.
 */
val EssenceSliver = card("Essence Sliver") {
    manaCost = "{3}{W}"
    typeLine = "Creature — Sliver"
    power = 3
    toughness = 3
    oracleText = "Whenever a Sliver deals damage, its controller gains that much life."

    val sliverFilter = GroupFilter(GameObjectFilter.Creature.withSubtype("Sliver"))

    staticAbility {
        ability = GrantTriggeredAbilityToCreatureGroup(
            ability = TriggeredAbility.create(
                trigger = Triggers.DealsDamage.event,
                binding = Triggers.DealsDamage.binding,
                effect = GainLifeEffect(DynamicAmount.TriggerDamageAmount)
            ),
            filter = sliverFilter
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "13"
        artist = "Glen Angus"
        flavorText = "The slivers would survive, even at the expense of every other creature on Otaria."
        imageUri = "https://cards.scryfall.io/normal/front/1/3/1346fa14-1d9f-4c6a-887d-d3a93de00743.jpg?1562898842"
    }
}
