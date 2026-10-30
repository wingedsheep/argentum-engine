package com.wingedsheep.mtg.sets.definitions.fut.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Sprout Swarm — Future Sight #138 (canonical printing)
 * {1}{G} · Instant
 *
 * Convoke
 * Buyback {3}
 * Create a 1/1 green Saproling creature token.
 */
val SproutSwarm = card("Sprout Swarm") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Convoke (Your creatures can help cast this spell. Each creature you tap while casting this spell pays for {1} or one mana of that creature's color.)\n" +
        "Buyback {3} (You may pay an additional {3} as you cast this spell. If you do, put this card into your hand as it resolves.)\n" +
        "Create a 1/1 green Saproling creature token."

    keywords(Keyword.CONVOKE)
    keywordAbility(KeywordAbility.buyback("{3}"))

    spell {
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Saproling"),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "138"
        artist = "Chippy"
        imageUri = "https://cards.scryfall.io/normal/front/0/b/0b915355-4e98-44df-81bd-961a3d3c86b8.jpg?1783943098"
    }
}
