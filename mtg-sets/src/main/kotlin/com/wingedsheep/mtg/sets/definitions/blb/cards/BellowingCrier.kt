package com.wingedsheep.mtg.sets.definitions.blb.cards

import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.dsl.HandPatterns

/**
 * Bellowing Crier
 * {1}{U}
 * Creature — Frog Advisor
 * 2/1
 *
 * When this creature enters, draw a card, then discard a card.
 */
val BellowingCrier = card("Bellowing Crier") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Frog Advisor"
    power = 2
    toughness = 1
    oracleText = "When this creature enters, draw a card, then discard a card."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = HandPatterns.loot()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "42"
        artist = "Jeff Miracola"
        flavorText = "Glarb's court can always be found repeating his disastrous visions, even when they're unwelcome."
        imageUri = "https://cards.scryfall.io/normal/front/c/a/ca2215dd-6300-49cf-b9b2-3a840b786c31.jpg?1721426026"
    }
}
