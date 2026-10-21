package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.becomesDayAsEntersIfNeither
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Firmament Sage
 * {3}{U}
 * Creature — Human Wizard
 * 2/3
 * If it's neither day nor night, it becomes day as this creature enters.
 * Whenever day becomes night or night becomes day, draw a card.
 */
val FirmamentSage = card("Firmament Sage") {
    manaCost = "{3}{U}"
    typeLine = "Creature — Human Wizard"
    power = 2
    toughness = 3
    oracleText = "If it's neither day nor night, it becomes day as this creature enters.\n" +
        "Whenever day becomes night or night becomes day, draw a card."

    becomesDayAsEntersIfNeither()

    triggeredAbility {
        trigger = Triggers.dayBecomesNightOrNightBecomesDay
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "53"
        artist = "Anastasia Ovchinnikova"
        flavorText = "The cause remained unclear, but the fact was undeniable: nights were lengthening faster than in any autumn past."
        imageUri = "https://cards.scryfall.io/normal/front/9/d/9db186a1-3dc5-4ed2-8cd4-1e4f13094885.jpg?1783925641"
    }
}
