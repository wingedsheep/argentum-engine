package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Oreskos Sun Guide
 * {1}{W}
 * Creature — Cat Monk
 * 2/2
 *
 * Inspired — Whenever this creature becomes untapped, you gain 2 life.
 *
 * Canonical printing: Born of the Gods, the card's earliest real printing.
 */
val OreskosSunGuide = card("Oreskos Sun Guide") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Cat Monk"
    power = 2
    toughness = 2
    oracleText = "Inspired — Whenever this creature becomes untapped, you gain 2 life."

    triggeredAbility {
        trigger = Triggers.self.becomesUntapped()
        effect = Effects.GainLife(2)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "22"
        artist = "Mathias Kollros"
        flavorText = "\"Let the humans have their pantheon. We need no gods to thrive. Even a mortal such as I " +
            "can capture a part of the sun's power.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/6/064435e7-20ea-4ba4-b039-a945afeeb5c6.jpg?1783939578"
    }
}
