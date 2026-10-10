package com.wingedsheep.mtg.sets.definitions.usg.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantAdditionalLandDrop

/**
 * Exploration
 * {G}
 * Enchantment
 * You may play an additional land on each of your turns.
 *
 * The extra land drop is a static [GrantAdditionalLandDrop] (cumulative with other such effects).
 */
val Exploration = card("Exploration") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Enchantment"
    oracleText = "You may play an additional land on each of your turns."

    staticAbility {
        ability = GrantAdditionalLandDrop(count = 1)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "250"
        artist = "Brian Snõddy"
        flavorText = "The first explorers found Argoth a storehouse of natural wealth—towering forests " +
            "grown over rich veins of ore."
        imageUri = "https://cards.scryfall.io/normal/front/2/f/2f09e451-0246-45a2-8bfd-07d3c65ddfe6.jpg?1783946316"
    }
}
