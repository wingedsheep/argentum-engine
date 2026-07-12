package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Falkenrath Celebrants
 * {4}{R}
 * Creature — Vampire
 * 4/4
 *
 * Menace
 * When this creature enters, create two Blood tokens.
 */
val FalkenrathCelebrants = card("Falkenrath Celebrants") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Vampire"
    power = 4
    toughness = 4
    oracleText = "Menace (This creature can't be blocked except by two or more creatures.)\n" +
        "When this creature enters, create two Blood tokens. (They're artifacts with \"{1}, {T}, " +
        "Discard a card, Sacrifice this token: Draw a card.\")"

    keywords(Keyword.MENACE)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.CreateBlood(2)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "306"
        artist = "Zinna Du"
        imageUri = "https://cards.scryfall.io/normal/front/6/a/6aacc8fa-9440-494b-a3d9-7b1678c5a9f6.jpg?1782702979"
    }
}
