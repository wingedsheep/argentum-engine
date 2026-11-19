package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.RestrictCardsDrawnPerTurn
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Spirit of the Labyrinth — a 3/1 whose static caps every player, its controller included,
 * at one card drawn per turn.
 */
val SpiritOfTheLabyrinth = card("Spirit of the Labyrinth") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment Creature — Spirit"
    power = 3
    toughness = 1
    oracleText = "Each player can't draw more than one card each turn."

    staticAbility {
        ability = RestrictCardsDrawnPerTurn(affected = Player.Each, maxPerTurn = 1)
    }

    metadata {
        ruling("2014-02-01", "If you haven't drawn any cards in a turn, and a spell or ability would cause you to draw multiple cards, you'll just draw one card.")
        ruling("2014-02-01", "If you draw a card, and then Spirit of the Labyrinth enters the battlefield, you won't be able to draw more cards that turn.")
        ruling("2014-02-01", "If a replacement effect would try to replace a card that you can't draw, that effect can't apply.")
        ruling("2014-02-01", "You can draw a maximum of one card on each player's turn. Subsequent card draws are ignored.")
        rarity = Rarity.RARE
        collectorNumber = "27"
        artist = "Jason Chan"
        flavorText = "Students at the Dekatia Academy learn that being sent to study with her is a lesson in itself."
        imageUri = "https://cards.scryfall.io/normal/front/f/4/f44e5128-e146-4e46-b313-a40d82719d1d.jpg?1783939575"
    }
}
