package com.wingedsheep.mtg.sets.definitions.mbs.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.SelectionMode

/**
 * Lead the Stampede — Mirrodin Besieged #82 (canonical / earliest real printing, 2011)
 * {2}{G} · Sorcery
 *
 * Look at the top five cards of your library. You may reveal any number of creature cards from
 * among them and put the revealed cards into your hand. Put the rest on the bottom of your library
 * in any order.
 *
 * The shared [Patterns.Library.lookAtTopAndTakeMatching] dig with [SelectionMode.ChooseAnyNumber]
 * (zero is allowed — the "may"), kept cards revealed as they go to hand, and "in any order" as
 * [CardOrder.ControllerChooses] for the remainder.
 */
val LeadTheStampede = card("Lead the Stampede") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Look at the top five cards of your library. You may reveal any number of creature " +
        "cards from among them and put the revealed cards into your hand. Put the rest on the bottom " +
        "of your library in any order."

    spell {
        effect = Patterns.Library.lookAtTopAndTakeMatching(
            count = DynamicAmounts.fixed(5),
            filter = GameObjectFilter.Creature,
            prompt = "You may reveal any number of creature cards and put them into your hand",
            selection = SelectionMode.ChooseAnyNumber,
            keepRevealed = true,
            restOrder = CardOrder.ControllerChooses,
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "82"
        artist = "Efrem Palacios"
        imageUri = "https://cards.scryfall.io/normal/front/6/6/66ed14c8-38c6-4da5-a6ee-f814478161d2.jpg?1783941375"
    }
}
