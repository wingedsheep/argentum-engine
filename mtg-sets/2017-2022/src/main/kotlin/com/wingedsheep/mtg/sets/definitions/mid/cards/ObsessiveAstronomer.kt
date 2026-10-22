package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.becomesDayAsEntersIfNeither
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Obsessive Astronomer
 * {1}{R}
 * Creature — Human Wizard
 * 2/2
 * If it's neither day nor night, it becomes day as this creature enters.
 * Whenever day becomes night or night becomes day, discard up to two cards, then draw that many cards.
 *
 * The day/night trigger is the [FirmamentSage] shape; the payoff is the Tersa Lightshatter
 * "discard up to two, then draw that many" loot ([Patterns.Hand.discardUpToThenDraw]).
 */
val ObsessiveAstronomer = card("Obsessive Astronomer") {
    manaCost = "{1}{R}"
    typeLine = "Creature — Human Wizard"
    power = 2
    toughness = 2
    oracleText = "If it's neither day nor night, it becomes day as this creature enters.\n" +
        "Whenever day becomes night or night becomes day, discard up to two cards, then draw that many cards."

    becomesDayAsEntersIfNeither()

    triggeredAbility {
        trigger = Triggers.dayBecomesNightOrNightBecomesDay
        effect = Patterns.Hand.discardUpToThenDraw(2)
        description = "Whenever day becomes night or night becomes day, discard up to two cards, then draw that many cards."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "152"
        artist = "Josu Hernaiz"
        flavorText = "\"Sleep? I can sleep when I'm dead!\""
        imageUri = "https://cards.scryfall.io/normal/front/4/f/4f6e0cb6-283b-448e-89c6-8b6e8e21e38b.jpg?1783925594"
    }
}
