package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.SelectionMode

/**
 * Uncovered Clues
 * {2}{U}
 * Sorcery
 *
 * Look at the top four cards of your library. You may reveal up to two instant and/or sorcery
 * cards from among them and put the revealed cards into your hand. Put the rest on the bottom of
 * your library in any order.
 *
 * [Patterns.Library.lookAtTopAndTakeMatching] with an up-to-two instant-or-sorcery selection that
 * is revealed as it goes to hand, and the rest bottomed in the controller's chosen order.
 */
val UncoveredClues = card("Uncovered Clues") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Look at the top four cards of your library. You may reveal up to two instant and/or sorcery " +
        "cards from among them and put the revealed cards into your hand. Put the rest on the bottom of your " +
        "library in any order."

    spell {
        effect = Patterns.Library.lookAtTopAndTakeMatching(
            count = DynamicAmounts.fixed(4),
            filter = GameObjectFilter.InstantOrSorcery,
            prompt = "You may reveal up to two instant and/or sorcery cards and put them into your hand",
            selection = SelectionMode.ChooseUpTo(DynamicAmounts.fixed(2)),
            keepRevealed = true,
            restOrder = CardOrder.ControllerChooses,
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "19"
        artist = "Jaime Jones"
        flavorText = "The Implicit Maze winds through Ravnica, connecting arcane landmarks of all ten guilds."
        imageUri = "https://cards.scryfall.io/normal/front/9/d/9dd24556-994f-4480-835e-11d4443f0700.jpg?1783940040"
    }
}
