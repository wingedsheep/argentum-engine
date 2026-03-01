package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect

/**
 * Hordeling Outburst
 * {1}{R}{R}
 * Sorcery
 * Create three 1/1 red Goblin creature tokens.
 */
val HordelingOutburst = card("Hordeling Outburst") {
    manaCost = "{1}{R}{R}"
    typeLine = "Sorcery"
    oracleText = "Create three 1/1 red Goblin creature tokens."

    spell {
        effect = CreateTokenEffect(
            count = 3,
            power = 1,
            toughness = 1,
            colors = setOf(Color.RED),
            creatureTypes = setOf("Goblin"),
            imageUri = "https://cards.scryfall.io/normal/front/e/d/ed418a8b-f158-492d-a323-6265b3175292.jpg?1562640121"
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "111"
        artist = "Zoltan Boros"
        flavorText = "\"Leave no scraps, lest you attract pests.\"\n—Mardu threat"
        imageUri = "https://cards.scryfall.io/normal/front/a/5/a5c1bf52-2737-423a-b340-07448afcaea6.jpg?1562791519"
    }
}

