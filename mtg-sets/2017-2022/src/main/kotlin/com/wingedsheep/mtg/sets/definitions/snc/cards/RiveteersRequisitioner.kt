package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Riveteers Requisitioner
 * {1}{R}
 * Creature — Lizard Rogue
 * 3 / 1
 * When this creature dies, create a Treasure token. (It's an artifact with "{T}, Sacrifice this token: Add one mana of any color.")
 * Blitz {2}{R} (If you cast this spell for its blitz cost, it gains haste and "When this creature dies, draw a card." Sacrifice it at the beginning of the next end step.)
 */
val RiveteersRequisitioner = card("Riveteers Requisitioner") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Lizard Rogue"
    oracleText = "When this creature dies, create a Treasure token. (It's an artifact with \"{T}, Sacrifice this token: Add one mana of any color.\")\nBlitz {2}{R} (If you cast this spell for its blitz cost, it gains haste and \"When this creature dies, draw a card.\" Sacrifice it at the beginning of the next end step.)"
    power = 3
    toughness = 1

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.CreateTreasure()
    }

    keywordAbility(KeywordAbility.blitz("{2}{R}"))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "121"
        artist = "Mark Behm"
        imageUri = "https://cards.scryfall.io/normal/front/8/a/8ab5152d-767a-47d5-acfc-f439810aaeea.jpg?1783923114"
    }
}
