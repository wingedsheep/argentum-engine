package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AdditionalManaOnLandTap
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeywordToCreatureGroup
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Lavaleaper
 * {3}{R}
 * Creature — Elemental
 * 4/4
 * All creatures have haste.
 * Whenever a player taps a basic land for mana, that player adds one mana of any type that land produced.
 */
val Lavaleaper = card("Lavaleaper") {
    manaCost = "{3}{R}"
    typeLine = "Creature — Elemental"
    power = 4
    toughness = 4
    oracleText = "All creatures have haste.\n" +
        "Whenever a player taps a basic land for mana, that player adds one mana of any type that land produced."

    staticAbility {
        ability = GrantKeywordToCreatureGroup(
            keyword = Keyword.HASTE,
            filter = GroupFilter.AllCreatures
        )
    }

    staticAbility {
        ability = AdditionalManaOnLandTap(
            filter = GameObjectFilter.BasicLand
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "150"
        artist = "Ron Spears"
        flavorText = "Flamekin believe it knows the slopes of Mount Kulrath even better than they do."
        imageUri = "https://cards.scryfall.io/normal/front/8/2/82902488-d178-4752-bcfb-dd3050654d23.jpg?1767658312"
    }
}
