package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Comparative Analysis
 * {3}{U}
 * Instant
 * Surge {2}{U} (You may cast this spell for its surge cost if you or a teammate has cast another spell this turn.)
 * Target player draws two cards.
 */
val ComparativeAnalysis = card("Comparative Analysis") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Surge {2}{U} (You may cast this spell for its surge cost if you or a teammate has cast another spell this turn.)\n" +
        "Target player draws two cards."

    keywordAbility(KeywordAbility.surge("{2}{U}"))

    spell {
        val t = target(Targets.Player)
        effect = Effects.DrawCards(2, t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "51"
        artist = "Willian Murai"
        flavorText = "\"Our time grows short. If we wish to vanquish the Eldrazi, we must unlock the hedrons' secrets.\"\n—Jace Beleren"
        imageUri = "https://cards.scryfall.io/normal/front/d/d/dd83129b-7e8c-4cc5-a7b3-e0ae221d7ad4.jpg?1783937919"
    }
}
