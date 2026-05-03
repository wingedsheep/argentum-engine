package com.wingedsheep.mtg.sets.definitions.edgeofeternities.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects

/**
 * Illvoi Galeblade
 * {U}
 * Creature — Jellyfish Warrior
 * Flash
 * Flying
 * {2}, Sacrifice this creature: Draw a card.
 */
val IllvoiGaleblade = card("Illvoi Galeblade") {
    manaCost = "{U}"
    typeLine = "Creature — Jellyfish Warrior"
    power = 1
    toughness = 1
    oracleText = "Flash\nFlying\n{2}, Sacrifice this creature: Draw a card."

    // Flash and Flying keywords
    keywords(Keyword.FLASH, Keyword.FLYING)

    // Activated ability: sacrifice to draw a card
    activatedAbility {
        cost = com.wingedsheep.sdk.dsl.Costs.Composite(
            com.wingedsheep.sdk.dsl.Costs.Mana("{2}"),
            com.wingedsheep.sdk.dsl.Costs.SacrificeSelf
        )
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "58"
        artist = "Nathaniel Himawan"
        flavorText = "Uthros guards have such rapid reflexes that it has led to rumors of experimental precognition."
        imageUri = "https://cards.scryfall.io/normal/front/7/6/769f7d13-a312-4d79-8639-6ae248452448.jpg?1752946779"
    }
}
