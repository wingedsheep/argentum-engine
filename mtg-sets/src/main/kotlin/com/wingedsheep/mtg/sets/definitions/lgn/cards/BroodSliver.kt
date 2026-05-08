package com.wingedsheep.mtg.sets.definitions.lgn.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantTriggeredAbility
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Brood Sliver
 * {4}{G}
 * Creature — Sliver
 * 3/3
 * Whenever a Sliver deals combat damage to a player, its controller may create a 1/1 colorless Sliver creature token.
 */
val BroodSliver = card("Brood Sliver") {
    manaCost = "{4}{G}"
    typeLine = "Creature — Sliver"
    power = 3
    toughness = 3
    oracleText = "Whenever a Sliver deals combat damage to a player, its controller may create a 1/1 colorless Sliver creature token."

    val sliverFilter = GroupFilter(GameObjectFilter.Creature.withSubtype("Sliver"))

    staticAbility {
        ability = GrantTriggeredAbility(
            ability = TriggeredAbility.create(
                trigger = Triggers.DealsCombatDamageToPlayer.event,
                binding = Triggers.DealsCombatDamageToPlayer.binding,
                effect = Effects.CreateToken(
                    power = 1,
                    toughness = 1,
                    colors = emptySet(),
                    creatureTypes = setOf("Sliver")
                ),
                optional = true
            ),
            filter = sliverFilter
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "120"
        artist = "Ron Spears"
        flavorText = "Within weeks, more slivers nested in Otaria than ever existed on Rath."
        imageUri = "https://cards.scryfall.io/normal/front/3/3/33803c12-1d78-49fe-a3a3-7f47c60a96b6.jpg?1562905370"
    }
}
