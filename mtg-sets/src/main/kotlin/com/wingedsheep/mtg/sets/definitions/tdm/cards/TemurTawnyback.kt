package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.dsl.HandPatterns

/**
 * Temur Tawnyback — Tarkir: Dragonstorm #229
 * {2/G}{2/U}{2/R} · Creature — Beast · 4/3
 *
 * When this creature enters, draw a card, then discard a card.
 *
 * Straight loot ETB via [HandPatterns.loot] (draw a card, then discard a card). The mana
 * cost is monocolored hybrid ("twobrid"): each symbol can be paid with 2 generic or one
 * mana of the listed color, so it's castable in any of G/U/R decks.
 */
val TemurTawnyback = card("Temur Tawnyback") {
    manaCost = "{2/G}{2/U}{2/R}"
    colorIdentity = "GUR"
    typeLine = "Creature — Beast"
    power = 4
    toughness = 3
    oracleText = "When this creature enters, draw a card, then discard a card."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = HandPatterns.loot()
        description = "When this creature enters, draw a card, then discard a card."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "229"
        artist = "Brian Valeza"
        flavorText = "\"He just kind of followed me home one day. I mean, could you say no to this face?\"\n" +
            "—Karasi, Mistrise provisioner"
        imageUri = "https://cards.scryfall.io/normal/front/3/c/3cdb383f-bc04-46d1-aa3a-7459d57f1fec.jpg?1743204904"
    }
}
