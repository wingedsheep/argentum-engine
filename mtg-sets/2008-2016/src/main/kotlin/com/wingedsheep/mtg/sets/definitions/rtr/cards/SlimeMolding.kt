package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Slime Molding
 * {X}{G}
 * Sorcery
 *
 * Create an X/X green Ooze creature token.
 */
val SlimeMolding = card("Slime Molding") {
    manaCost = "{X}{G}"
    typeLine = "Sorcery"
    oracleText = "Create an X/X green Ooze creature token."

    spell {
        val x = DynamicAmounts.xValue()
        effect = Effects.CreateDynamicToken(
            dynamicPower = x,
            dynamicToughness = x,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Ooze"),
            imageUri = "https://cards.scryfall.io/normal/front/a/6/a686e51d-e8c3-4ed6-8357-c3d38d56dd09.jpg?1783940381",
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "135"
        artist = "Marco Nelor"
        flavorText = "\"Give me enough refuse and mana and I will summon an ooze that can engulf all of Ravnica.\"\n—Cevraya, Golgari shaman"
        imageUri = "https://cards.scryfall.io/normal/front/4/4/44a439e8-d586-4995-abfc-3dee5c860968.jpg?1783940346"
    }
}
