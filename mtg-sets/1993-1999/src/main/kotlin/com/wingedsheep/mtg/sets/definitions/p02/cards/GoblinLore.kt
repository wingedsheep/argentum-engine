package com.wingedsheep.mtg.sets.definitions.p02.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Goblin Lore
 * {1}{R}
 * Sorcery
 * Draw four cards, then discard three cards at random.
 */
val GoblinLore = card("Goblin Lore") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Draw four cards, then discard three cards at random."

    spell {
        effect = Effects.DrawCards(4) then Patterns.Hand.discardRandom(3)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "99"
        artist = "D. Alexander Gregory"
        flavorText = "\"I done forgot more than you'll ever know, pipsqueak.\"\n\"Yeah—that's your problem.\""
        imageUri = "https://cards.scryfall.io/normal/front/f/2/f26f34db-732c-43d4-a6ef-e170538c0235.jpg?1783946465"
    }
}
