package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Revel Ruiner
 * {3}{B}
 * Creature — Octopus Rogue
 * 3 / 1
 * Menace (This creature can't be blocked except by two or more creatures.)
 * When this creature enters, it connives.
 */
val RevelRuiner = card("Revel Ruiner") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Octopus Rogue"
    oracleText = "Menace (This creature can't be blocked except by two or more creatures.)\n" +
        "When this creature enters, it connives. (Draw a card, then discard a card. If you discarded a nonland card, put a +1/+1 counter on this creature.)"
    power = 3
    toughness = 1

    keywords(Keyword.MENACE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Connive()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "91"
        artist = "Christina Davis"
        imageUri = "https://cards.scryfall.io/normal/front/e/3/e3fbe7f0-8a91-4832-8953-54ab3669372c.jpg"
    }
}
