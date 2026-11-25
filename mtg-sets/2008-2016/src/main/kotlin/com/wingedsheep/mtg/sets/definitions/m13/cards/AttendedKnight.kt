package com.wingedsheep.mtg.sets.definitions.m13.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Attended Knight
 * {2}{W}
 * Creature — Human Knight
 * 2/2
 * First strike
 * When this creature enters, create a 1/1 white Soldier creature token.
 */
val AttendedKnight = card("Attended Knight") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Knight"
    oracleText = "First strike\n" +
        "When this creature enters, create a 1/1 white Soldier creature token."
    power = 2
    toughness = 2

    keywords(Keyword.FIRST_STRIKE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Soldier"),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "5"
        artist = "Seb McKinnon"
        imageUri = "https://cards.scryfall.io/normal/front/c/0/c0f5cb3f-c27d-4b35-930f-00d806393796.jpg"
    }
}
