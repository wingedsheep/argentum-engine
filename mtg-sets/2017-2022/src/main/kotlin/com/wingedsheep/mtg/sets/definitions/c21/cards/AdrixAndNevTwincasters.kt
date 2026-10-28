package com.wingedsheep.mtg.sets.definitions.c21.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.MultiplyTokenCreation
import com.wingedsheep.sdk.scripting.effects.WardCost

/**
 * Adrix and Nev, Twincasters
 * {2}{G}{U}
 * Legendary Creature — Merfolk Wizard
 * 2/2
 * Ward {2}
 * If one or more tokens would be created under your control, twice that many of those tokens are
 * created instead.
 *
 * Ward is the [KeywordAbility.Ward] keyword ability; the token clause is Parallel Lives' default
 * [MultiplyTokenCreation] (factor 2, tokens created under your control).
 */
val AdrixAndNevTwincasters = card("Adrix and Nev, Twincasters") {
    manaCost = "{2}{G}{U}"
    colorIdentity = "GU"
    typeLine = "Legendary Creature — Merfolk Wizard"
    power = 2
    toughness = 2
    oracleText = "Ward {2} (Whenever this creature becomes the target of a spell or ability an opponent controls, counter it unless that player pays {2}.)\n" +
        "If one or more tokens would be created under your control, twice that many of those tokens are created instead."

    keywordAbility(KeywordAbility.Ward(WardCost.Mana("{2}")))

    replacementEffect(MultiplyTokenCreation())

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "9"
        artist = "Andrew Mar"
        imageUri = "https://cards.scryfall.io/normal/front/d/4/d495e084-92ec-4675-b6aa-8ade2775b796.jpg?1783927610"
        ruling(
            "2021-06-18",
            "Everything that is specified by the effect creating the original token or tokens will also be true about the additional token or tokens created by Adrix and Nev's replacement effect. For example, if an effect tells you to create a token \"tapped and attacking\" the additional tokens will also be tapped and attacking. Similarly, if an effect creates a token and puts counters on it (such as a Fractal token) the additional token will also get those counters.",
        )
    }
}
