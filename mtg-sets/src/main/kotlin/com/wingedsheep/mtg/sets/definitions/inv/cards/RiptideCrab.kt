package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Riptide Crab
 * {1}{W}{U}
 * Creature — Crab
 * 1/3
 * Vigilance
 * When this creature dies, draw a card.
 */
val RiptideCrab = card("Riptide Crab") {
    manaCost = "{1}{W}{U}"
    colorIdentity = "WU"
    typeLine = "Creature — Crab"
    power = 1
    toughness = 3
    oracleText = "Vigilance\nWhen this creature dies, draw a card."

    keywords(Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.Dies
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "266"
        artist = "David Martin"
        imageUri = "https://cards.scryfall.io/normal/front/7/e/7e42ae1d-62b4-4b19-aafc-f12bdd6fb8cc.jpg?1562920483"
    }
}
