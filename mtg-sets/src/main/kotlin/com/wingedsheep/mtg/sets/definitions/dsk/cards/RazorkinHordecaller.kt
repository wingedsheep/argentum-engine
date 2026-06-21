package com.wingedsheep.mtg.sets.definitions.dsk.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Razorkin Hordecaller
 * {4}{R}
 * Creature — Human Clown Berserker
 * 4/4
 *
 * Haste
 * Whenever you attack, create a 1/1 red Gremlin creature token.
 */
val RazorkinHordecaller = card("Razorkin Hordecaller") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Clown Berserker"
    power = 4
    toughness = 4
    oracleText = "Haste\nWhenever you attack, create a 1/1 red Gremlin creature token."

    keywords(Keyword.HASTE)

    // Whenever you attack, create a 1/1 red Gremlin creature token.
    triggeredAbility {
        trigger = Triggers.YouAttack
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.RED),
            creatureTypes = setOf("Gremlin"),
            imageUri = "https://cards.scryfall.io/normal/front/d/9/d948b503-890a-49d5-a3cf-cb6e604851b8.jpg?1726236661",
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "152"
        artist = "David Álvarez"
        flavorText = "\"Know your place, pets! First blood is mine!\""
        imageUri = "https://cards.scryfall.io/normal/front/f/7/f7fb0f11-d1d0-4941-a1a7-a2db88f30394.jpg?1726286414"
    }
}
