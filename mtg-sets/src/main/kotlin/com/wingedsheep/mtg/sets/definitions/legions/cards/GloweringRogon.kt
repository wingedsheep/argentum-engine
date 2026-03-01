package com.wingedsheep.mtg.sets.definitions.legions.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AmplifyEffect

/**
 * Glowering Rogon
 * {5}{G}
 * Creature — Beast
 * 4/4
 * Amplify 1 (As this creature enters, put a +1/+1 counter on it for each
 * Beast card you reveal in your hand.)
 */
val GloweringRogon = card("Glowering Rogon") {
    manaCost = "{5}{G}"
    typeLine = "Creature — Beast"
    power = 4
    toughness = 4
    oracleText = "Amplify 1 (As this creature enters, put a +1/+1 counter on it for each Beast card you reveal in your hand.)"

    keywords(Keyword.AMPLIFY)

    replacementEffect(AmplifyEffect(countersPerReveal = 1))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "128"
        artist = "Kev Walker"
        flavorText = "A herd of one."
        imageUri = "https://cards.scryfall.io/normal/front/9/7/974b0881-bd26-4074-93dd-a1e3600347c4.jpg?1562925487"
    }
}
