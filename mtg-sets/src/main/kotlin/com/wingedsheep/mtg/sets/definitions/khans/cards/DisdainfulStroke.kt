package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Disdainful Stroke
 * {1}{U}
 * Instant
 * Counter target spell with mana value 4 or greater.
 */
val DisdainfulStroke = card("Disdainful Stroke") {
    manaCost = "{1}{U}"
    typeLine = "Instant"
    oracleText = "Counter target spell with mana value 4 or greater."

    spell {
        target = Targets.SpellWithManaValueAtLeast(4)
        effect = Effects.CounterSpell()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "37"
        artist = "Svetlin Velinov"
        flavorText = "\"You are beneath contempt. Your lineage will be forgotten.\""
        imageUri = "https://cards.scryfall.io/normal/front/1/8/180425c9-1898-48d4-9932-ddfb1a28e6b0.jpg?1562783110"
    }
}
