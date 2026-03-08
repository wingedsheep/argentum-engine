package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Empty the Pits
 * {X}{X}{B}{B}{B}{B}
 * Instant
 * Delve
 * Create X tapped 2/2 black Zombie creature tokens.
 */
val EmptyThePits = card("Empty the Pits") {
    manaCost = "{X}{X}{B}{B}{B}{B}"
    typeLine = "Instant"
    oracleText = "Delve (Each card you exile from your graveyard while casting this spell pays for {1}.)\nCreate X tapped 2/2 black Zombie creature tokens."

    keywords(Keyword.DELVE)

    spell {
        effect = CreateTokenEffect(
            count = DynamicAmount.XValue,
            power = 2,
            toughness = 2,
            colors = setOf(Color.BLACK),
            creatureTypes = setOf("Zombie"),
            tapped = true
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "72"
        artist = "Ryan Alexander Lee"
        flavorText = "The Sultai would rebuild the empire on the backs of the dead."
        imageUri = "https://cards.scryfall.io/normal/front/e/9/e94b1ac7-dd7b-46a1-a74e-aa0563bab3cd.jpg?1562795355"
    }
}
