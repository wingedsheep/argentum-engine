package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * You Meet in a Tavern
 * {2}{G}{G}
 * Sorcery
 *
 * Choose one —
 * • Form a Party — Look at the top five cards of your library. You may reveal any number of
 *   creature cards from among them and put them into your hand. Put the rest on the bottom of
 *   your library in a random order.
 * • Start a Brawl — Creatures you control get +2/+2 until end of turn.
 *
 * A plain choose-one `modal`: Forging the Anchor's dig ([Patterns.Library.lookAtTopAndTakeMatching]
 * with [SelectionMode.ChooseAnyNumber], kept cards revealed, rest to the bottom in a random order)
 * filtered to creatures, and the untargeted team pump.
 */
val YouMeetInATavern = card("You Meet in a Tavern") {
    manaCost = "{2}{G}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Choose one —\n" +
        "• Form a Party — Look at the top five cards of your library. You may reveal any number of " +
        "creature cards from among them and put them into your hand. Put the rest on the bottom of " +
        "your library in a random order.\n" +
        "• Start a Brawl — Creatures you control get +2/+2 until end of turn."

    spell {
        modal(chooseCount = 1) {
            mode(
                "Form a Party — Look at the top five cards of your library. You may reveal any number " +
                    "of creature cards from among them and put them into your hand. Put the rest on the " +
                    "bottom of your library in a random order.",
                Patterns.Library.lookAtTopAndTakeMatching(
                    count = DynamicAmounts.fixed(5),
                    filter = GameObjectFilter.Creature,
                    prompt = "You may reveal any number of creature cards and put them into your hand",
                    selection = SelectionMode.ChooseAnyNumber,
                    keepRevealed = true,
                )
            )
            mode(
                "Start a Brawl — Creatures you control get +2/+2 until end of turn.",
                Patterns.Group.modifyStatsForAll(2, 2, GroupFilter.AllCreaturesYouControl)
            )
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "215"
        artist = "Zoltan Boros"
        imageUri = "https://cards.scryfall.io/normal/front/5/9/593aa59a-4025-4df8-9f27-188fc7712fde.jpg?1783926450"
    }
}
