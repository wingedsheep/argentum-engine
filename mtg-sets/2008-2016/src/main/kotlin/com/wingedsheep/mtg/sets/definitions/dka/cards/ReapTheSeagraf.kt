package com.wingedsheep.mtg.sets.definitions.dka.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Reap the Seagraf
 * {2}{B}
 * Sorcery
 * Create a 2/2 black Zombie creature token.
 * Flashback {4}{U}
 */
val ReapTheSeagraf = card("Reap the Seagraf") {
    manaCost = "{2}{B}"
    colorIdentity = "UB"
    typeLine = "Sorcery"
    oracleText = "Create a 2/2 black Zombie creature token.\n" +
        "Flashback {4}{U} (You may cast this card from your graveyard for its flashback cost. Then exile it.)"

    spell {
        effect = Effects.CreateToken(
            power = 2,
            toughness = 2,
            colors = setOf(Color.BLACK),
            creatureTypes = setOf("Zombie")
        )
    }
    keywordAbility(KeywordAbility.flashback("{4}{U}"))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "72"
        artist = "James Ryman"
        flavorText = "The wreck hung on the jagged reef, each night disgorging more of its gruesome cargo."
        imageUri = "https://cards.scryfall.io/normal/front/4/d/4defdead-19fa-4535-9f71-8808388b0332.jpg"
    }
}
