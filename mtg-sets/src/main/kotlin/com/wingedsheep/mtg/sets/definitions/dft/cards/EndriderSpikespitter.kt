package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.maxSpeed
import com.wingedsheep.sdk.dsl.startYourEngines
import com.wingedsheep.sdk.model.Rarity

/**
 * Endrider Spikespitter — Aetherdrift #125
 * {3}{R} · Creature — Human Mercenary · 3/4
 *
 * Reach
 * Start your engines!
 * Max speed — At the beginning of your upkeep, exile the top card of your library. You may play
 * that card this turn.
 *
 * The upkeep ability is declared inside a [maxSpeed] block, which folds "your speed is 4" into the
 * ability's `triggerCondition` (CR 603.4) — so it is checked both when the trigger would fire and
 * again on resolution, and losing max speed in between correctly stops it. The effect is the
 * ordinary impulse-draw pattern ([Patterns.Exile.impulse]): exile the top card, then grant
 * permission to play it until end of turn. Per the Scryfall ruling the card is played normally,
 * paying its costs and following timing rules, which is exactly what `impulse` grants.
 */
val EndriderSpikespitter = card("Endrider Spikespitter") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Mercenary"
    oracleText = "Reach\n" +
        "Start your engines! (If you have no speed, it starts at 1. It increases once on each of " +
        "your turns when an opponent loses life. Max speed is 4.)\n" +
        "Max speed — At the beginning of your upkeep, exile the top card of your library. You may " +
        "play that card this turn."
    power = 3
    toughness = 4

    keywords(Keyword.REACH)

    startYourEngines()

    maxSpeed {
        triggeredAbility {
            trigger = Triggers.YourUpkeep
            effect = Patterns.Exile.impulse(1)
            description = "At the beginning of your upkeep, exile the top card of your library. " +
                "You may play that card this turn."
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "125"
        artist = "Mila Pesic"
        imageUri = "https://cards.scryfall.io/normal/front/4/e/4e58cb18-f216-4248-8f0d-65b0263c5c28.jpg?1783907883"
        ruling(
            "2025-02-07",
            "You must pay all costs and follow all normal timing rules for cards you play using " +
                "this creature's last ability."
        )
        ruling(
            "2025-02-07",
            "\"Max speed — [ability]\" means \"As long as you have max speed, this object has " +
                "[ability].\" If the granted ability functions in a zone other than the battlefield, " +
                "the max speed ability does too."
        )
    }
}
