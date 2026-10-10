package com.wingedsheep.mtg.sets.definitions.m11.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mayBeginGameOnBattlefield
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantFlashToSpellType

/**
 * Leyline of Anticipation — Magic 2011 #61 (canonical / earliest real-expansion printing).
 * {2}{U}{U} · Enchantment
 *
 * If this card is in your opening hand, you may begin the game with it on the battlefield.
 * You may cast spells as though they had flash.
 *
 * The opening-hand clause is the shared `mayBeginGameOnBattlefield()` helper (Leyline of the
 * Void). The flash grant is [GrantFlashToSpellType] over every spell, scoped to the controller —
 * the same static High Fae Trickster uses.
 */
val LeylineOfAnticipation = card("Leyline of Anticipation") {
    manaCost = "{2}{U}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment"
    oracleText = "If this card is in your opening hand, you may begin the game with it on the battlefield.\n" +
        "You may cast spells as though they had flash."

    mayBeginGameOnBattlefield()

    staticAbility {
        ability = GrantFlashToSpellType(
            filter = GameObjectFilter.Any,
            controllerOnly = true
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "61"
        artist = "Charles Urbach"
        imageUri = "https://cards.scryfall.io/normal/front/d/7/d7dbb092-3bb0-445e-ab26-d939cac92a73.jpg?1783941824"
        ruling(
            "2021-03-19",
            "A player's \"opening hand\" is the hand of cards the player has after all players have taken " +
                "mulligans. If players have any cards in hand that allow actions to be taken with them from a " +
                "player's opening hand, the starting player takes all such actions first in any order, followed " +
                "by each other player in turn order. Then the first turn begins."
        )
    }
}
