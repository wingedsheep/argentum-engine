package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Shadowbeast Sighting
 * {3}{G}
 * Sorcery
 * Create a 4/4 green Beast creature token.
 * Flashback {6}{G}
 */
val ShadowbeastSighting = card("Shadowbeast Sighting") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Create a 4/4 green Beast creature token.\n" +
        "Flashback {6}{G} (You may cast this card from your graveyard for its flashback cost. Then exile it.)"

    spell {
        effect = Effects.CreateToken(
            power = 4,
            toughness = 4,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Beast"),
            imageUri = "https://cards.scryfall.io/normal/front/f/5/f55c70b8-0fa5-4d08-9061-f53d6f949908.jpg?1783925222"
        )
    }

    keywordAbility(KeywordAbility.flashback("{6}{G}"))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "198"
        artist = "Piotr Foksowicz"
        flavorText = "\"Werewolves leave tracks. Vampires leave wrung-out corpses. This is something else.\"\n—Paulin, trapper of Somberwald"
        imageUri = "https://cards.scryfall.io/normal/front/1/b/1b12e978-4deb-4ccc-8ae6-462bc4598c90.jpg?1783925573"
    }
}
