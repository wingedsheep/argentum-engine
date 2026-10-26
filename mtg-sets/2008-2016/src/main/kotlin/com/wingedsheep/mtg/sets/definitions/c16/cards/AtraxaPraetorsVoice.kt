package com.wingedsheep.mtg.sets.definitions.c16.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Atraxa, Praetors' Voice
 * {G}{W}{U}{B}
 * Legendary Creature — Phyrexian Angel Horror
 * 4/4
 * Flying, vigilance, deathtouch, lifelink
 * At the beginning of your end step, proliferate.
 */
val AtraxaPraetorsVoice = card("Atraxa, Praetors' Voice") {
    manaCost = "{G}{W}{U}{B}"
    colorIdentity = "WUBG"
    typeLine = "Legendary Creature — Phyrexian Angel Horror"
    power = 4
    toughness = 4
    oracleText = "Flying, vigilance, deathtouch, lifelink\n" +
        "At the beginning of your end step, proliferate. (Choose any number of permanents and/or players, " +
        "then give each another counter of each kind already there.)"

    keywords(Keyword.FLYING, Keyword.VIGILANCE, Keyword.DEATHTOUCH, Keyword.LIFELINK)

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        effect = Effects.Proliferate()
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "28"
        artist = "Victor Adame Minguez"
        imageUri = "https://cards.scryfall.io/normal/front/c/3/c34ae834-775e-447a-a330-0270c227c667.jpg?1783937085"
    }
}
