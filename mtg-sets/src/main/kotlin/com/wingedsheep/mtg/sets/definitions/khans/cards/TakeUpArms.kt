package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect

/**
 * Take Up Arms
 * {4}{W}
 * Instant
 * Create three 1/1 white Warrior creature tokens.
 */
val TakeUpArms = card("Take Up Arms") {
    manaCost = "{4}{W}"
    typeLine = "Instant"
    oracleText = "Create three 1/1 white Warrior creature tokens."

    spell {
        effect = CreateTokenEffect(
            count = 3,
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Warrior"),
            imageUri = "https://cards.scryfall.io/normal/front/a/f/af4c9101-85bf-4a4f-a496-ff6db7b531b7.jpg?1562639979"
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "26"
        artist = "Craig J Spearing"
        flavorText = "\"Many scales make the skin of a dragon.\"\n—Abzan wisdom"
        imageUri = "https://cards.scryfall.io/normal/front/f/c/fc783f49-f58a-4783-87b4-4dbc7e896f2e.jpg?1562796534"
    }
}
