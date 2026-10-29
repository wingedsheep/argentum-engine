package com.wingedsheep.mtg.sets.definitions.zen.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Felidar Sovereign
 * {4}{W}{W}
 * Creature — Cat Beast
 * 4/6
 * Vigilance, lifelink
 * At the beginning of your upkeep, if you have 40 or more life, you win the game.
 *
 * The life check is an intervening "if" (CR 603.4): checked as the upkeep begins and again on
 * resolution, per the 2015-08-25 ruling.
 */
val FelidarSovereign = card("Felidar Sovereign") {
    manaCost = "{4}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Cat Beast"
    power = 4
    toughness = 6
    oracleText = "Vigilance (Attacking doesn't cause this creature to tap.)\n" +
        "Lifelink (Damage dealt by this creature also causes you to gain that much life.)\n" +
        "At the beginning of your upkeep, if you have 40 or more life, you win the game."

    keywords(Keyword.VIGILANCE, Keyword.LIFELINK)

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        interveningIf = Conditions.LifeAtLeast(40)
        effect = Effects.WinGame()
        description = "At the beginning of your upkeep, if you have 40 or more life, you win the game."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "12"
        artist = "Zoltan Boros & Gabor Szikszai"
        flavorText = "\"If survival is a game, I've seen the winner.\"\n—Hazir, Sejiri cartographer"
        imageUri = "https://cards.scryfall.io/normal/front/c/c/cc3c93e6-af75-4d7e-86b0-8a38425f51a1.jpg?1783942174"
        ruling("2015-08-25", "Felidar Sovereign's triggered ability checks to see if you have 40 or more life as your upkeep begins. If you don't, the ability won't trigger at all. If you do, the ability will check again as it tries to resolve. If you don't have 40 or more life at that time, the ability won't do anything.")
    }
}
