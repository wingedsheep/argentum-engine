package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Bat Whisperer
 * {3}{B}
 * Creature — Vampire
 * 4/2
 * When this creature enters, if an opponent lost life this turn, create a 1/1 black Bat creature
 * token with flying.
 *
 * Intervening-if ETB ([Conditions.OpponentLostLifeThisTurn], CR 603.4); the Bat uses MID's own
 * token art.
 */
val BatWhisperer = card("Bat Whisperer") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Vampire"
    power = 4
    toughness = 2
    oracleText = "When this creature enters, if an opponent lost life this turn, create a 1/1 black Bat " +
        "creature token with flying."

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.OpponentLostLifeThisTurn
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.BLACK),
            creatureTypes = setOf("Bat"),
            keywords = setOf(Keyword.FLYING),
            imageUri = "https://cards.scryfall.io/normal/front/e/5/e5c0f400-41be-488b-be84-b07289b1ef62.jpg?1783925225",
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "86"
        artist = "Jodie Muir"
        flavorText = "\"My little winged friends tell me which of these townsfolk are worth tasting for myself.\""
        imageUri = "https://cards.scryfall.io/normal/front/4/4/4412c901-0ec8-437a-9d1e-ce318bbe2b03.jpg"
    }
}
