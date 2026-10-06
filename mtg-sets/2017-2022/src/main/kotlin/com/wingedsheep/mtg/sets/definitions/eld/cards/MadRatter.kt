package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Mad Ratter
 * {3}{R}
 * Creature — Goblin
 * 1/2
 * Whenever you draw your second card each turn, create two 1/1 black Rat creature tokens.
 */
val MadRatter = card("Mad Ratter") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Goblin"
    power = 1
    toughness = 2
    oracleText = "Whenever you draw your second card each turn, create two 1/1 black Rat creature tokens."

    triggeredAbility {
        trigger = Triggers.you.drawsNth(2)
        effect = Effects.CreateToken(
            count = 2,
            power = 1,
            toughness = 1,
            colors = setOf(Color.BLACK),
            creatureTypes = setOf("Rat"),
            imageUri = "https://cards.scryfall.io/normal/front/e/4/e43a205e-43ea-4b3e-92ab-c2ee2172a50a.jpg?1783932482",
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "130"
        artist = "Johann Bodin"
        flavorText = "\"Gather round and tell me all from the courts and castles.\""
        imageUri = "https://cards.scryfall.io/normal/front/9/a/9a4cabcc-fb29-4bf2-b5ba-32b8c96aefd6.jpg?1783932620"
        ruling(
            "2019-10-04",
            "The triggered ability can trigger only once each turn. It doesn't matter whether the permanent " +
                "with that ability was on the battlefield when the first card was drawn. If it's not on the " +
                "battlefield when the second card is drawn, the ability can't trigger at all that turn. It " +
                "won't trigger when the third or fourth card is drawn."
        )
        ruling(
            "2019-10-04",
            "If a spell or ability causes you to put cards into your hand without specifically using the word " +
                "\"draw,\" it's not a card drawn."
        )
    }
}
