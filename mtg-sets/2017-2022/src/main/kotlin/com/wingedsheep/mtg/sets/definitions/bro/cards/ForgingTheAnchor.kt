package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SelectionMode

/**
 * Forging the Anchor
 * {2}{U}
 * Sorcery
 * Look at the top five cards of your library. You may reveal any number of artifact cards from among
 * them and put the revealed cards into your hand. Put the rest on the bottom of your library in a
 * random order.
 */
val ForgingTheAnchor = card("Forging the Anchor") {
    manaCost = "{2}{U}"
    typeLine = "Sorcery"
    oracleText = "Look at the top five cards of your library. You may reveal any number of artifact cards " +
        "from among them and put the revealed cards into your hand. Put the rest on the bottom of your " +
        "library in a random order."

    spell {
        effect = Patterns.Library.lookAtTopAndTakeMatching(
            count = DynamicAmounts.fixed(5),
            filter = GameObjectFilter.Artifact,
            prompt = "You may reveal any number of artifact cards and put them into your hand",
            selection = SelectionMode.ChooseAnyNumber,
            keepRevealed = true,
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "50"
        artist = "Liiga Smilshkalne"
        flavorText = "The metal turned and blossomed with Saheeli's will, each filigreed twist bringing the " +
            "temporal anchor closer to completion."
        imageUri = "https://cards.scryfall.io/normal/front/0/5/057f6add-1f1d-4cce-9c9d-5e59fb74d78e.jpg?1783920112"
    }
}
