package com.wingedsheep.mtg.sets.definitions.m11.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mayBeginGameOnBattlefield
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantHexproofToController

/**
 * Leyline of Sanctity — Magic 2011 #21 (canonical / earliest real-expansion printing).
 * {2}{W}{W} · Enchantment
 *
 * If this card is in your opening hand, you may begin the game with it on the battlefield.
 * You have hexproof.
 *
 * The opening-hand clause is the shared `mayBeginGameOnBattlefield()` helper (Leyline of the
 * Void); "You have hexproof" is the [GrantHexproofToController] static (Crystal Barricade,
 * Shalai, Voice of Plenty).
 */
val LeylineOfSanctity = card("Leyline of Sanctity") {
    manaCost = "{2}{W}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "If this card is in your opening hand, you may begin the game with it on the battlefield.\n" +
        "You have hexproof. (You can't be the target of spells or abilities your opponents control.)"

    mayBeginGameOnBattlefield()

    staticAbility {
        ability = GrantHexproofToController
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "21"
        artist = "Ryan Pancoast"
        imageUri = "https://cards.scryfall.io/normal/front/2/6/262de9ae-d641-4f0e-af6a-03ce0e1c91d3.jpg?1783941834"
        ruling(
            "2021-03-19",
            "A player's \"opening hand\" is the hand of cards the player has after all players have taken " +
                "mulligans. If players have any cards in hand that allow actions to be taken with them from a " +
                "player's opening hand, the starting player takes all such actions first in any order, followed " +
                "by each other player in turn order. Then the first turn begins."
        )
    }
}
