package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.EnteringAttackDefender

/**
 * Parhelion II — War of the Spark #24 (canonical printing)
 * {6}{W}{W} · Legendary Artifact — Vehicle · 5/5
 *
 * Flying, first strike, vigilance
 * Whenever Parhelion II attacks, create two 4/4 white Angel creature tokens with flying and
 * vigilance that are attacking.
 * Crew 4
 *
 * The Angels enter attacking but not tapped (the text says only "attacking"), and per the
 * ruling each one's defender is chosen separately — [EnteringAttackDefender.CONTROLLER_CHOOSES].
 */
val ParhelionII = card("Parhelion II") {
    manaCost = "{6}{W}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Artifact — Vehicle"
    power = 5
    toughness = 5
    oracleText = "Flying, first strike, vigilance\n" +
        "Whenever Parhelion II attacks, create two 4/4 white Angel creature tokens with flying and " +
        "vigilance that are attacking.\n" +
        "Crew 4 (Tap any number of creatures you control with total power 4 or more: This Vehicle " +
        "becomes an artifact creature until end of turn.)"

    keywords(Keyword.FLYING, Keyword.FIRST_STRIKE, Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.CreateToken(
            count = 2,
            power = 4,
            toughness = 4,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Angel"),
            keywords = setOf(Keyword.FLYING, Keyword.VIGILANCE),
            attacking = true,
            attackDefender = EnteringAttackDefender.CONTROLLER_CHOOSES,
            imageUri = "https://cards.scryfall.io/normal/front/b/1/b16b81e0-5525-4be3-96db-807146fdc5b3.jpg?1783933355",
        )
    }

    keywordAbility(KeywordAbility.crew(4))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "24"
        artist = "Adam Paquette"
        imageUri = "https://cards.scryfall.io/normal/front/2/6/261ba80e-3aca-423e-a0b7-19c2d4b44dbf.jpg?1783933478"
        ruling(
            "2019-05-03",
            "You choose which players or planeswalkers the two tokens are attacking. They don't have " +
                "to be attacking the same player or planeswalker that Parhelion II is attacking, and " +
                "they can each be attacking different players and/or planeswalkers."
        )
        ruling(
            "2019-05-03",
            "Although the tokens are attacking, they were never declared as attacking creatures (for " +
                "the purposes of abilities that trigger whenever a creature attacks, for example)."
        )
    }
}
