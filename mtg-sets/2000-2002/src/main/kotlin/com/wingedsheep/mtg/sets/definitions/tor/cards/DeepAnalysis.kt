package com.wingedsheep.mtg.sets.definitions.tor.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Deep Analysis
 * {3}{U}
 * Sorcery
 * Target player draws two cards.
 * Flashback—{1}{U}, Pay 3 life.
 */
val DeepAnalysis = card("Deep Analysis") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Target player draws two cards.\nFlashback—{1}{U}, Pay 3 life. (You may cast this card " +
        "from your graveyard for its flashback cost. Then exile it.)"

    spell {
        val t = target(Targets.Player)
        effect = Effects.DrawCards(2, t)
    }

    keywordAbility(KeywordAbility.flashback("{1}{U}", Costs.additional.PayLife(3)))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "36"
        artist = "Daren Bader"
        flavorText = "\"The specimen seems to be broken.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/1/01e3c2e9-d8df-4a7a-be86-7be8c6254fa2.jpg?1783945162"
    }
}
