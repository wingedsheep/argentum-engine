package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Run Ashore
 * {4}{U}{U}
 * Instant
 *
 * Choose one or both —
 * • The owner of target nonland permanent puts it on their choice of the top or bottom of their library.
 * • Return target nonland permanent to its owner's hand.
 *
 * "Choose one or both" is `modal(chooseCount = 2, minChooseCount = 1)` (CR 700.2). The first mode is
 * Moonsnare Prototype's owner-chooses placement ([Effects.PutOnTopOrBottomOfLibrary]); the second is a
 * plain bounce. Chosen modes resolve in printed order within the one resolution.
 */
val RunAshore = card("Run Ashore") {
    manaCost = "{4}{U}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Choose one or both —\n" +
        "• The owner of target nonland permanent puts it on their choice of the top or bottom of their library.\n" +
        "• Return target nonland permanent to its owner's hand."

    spell {
        modal(chooseCount = 2, minChooseCount = 1) {
            mode("Put target nonland permanent on the top or bottom of its owner's library (owner's choice)") {
                val t = target(TargetFilter.NonlandPermanent)
                effect = Effects.PutOnTopOrBottomOfLibrary(t)
            }
            mode("Return target nonland permanent to its owner's hand") {
                val t = target(TargetFilter.NonlandPermanent)
                effect = Effects.ReturnToHand(t)
            }
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "74"
        artist = "Svetlin Velinov"
        flavorText = "\"Which one of you slug-brained milksops forgot to make an offering to Cosima?\"\n—Jolkur, navigator"
        imageUri = "https://cards.scryfall.io/normal/front/7/e/7eed5a2d-d7a4-4f64-a96b-04c5d4206c5a.jpg?1783928257"

        ruling("2021-02-05", "For the first mode, the owner of the card chooses whether it goes on the top or bottom of that library.")
        ruling(
            "2021-02-05",
            "If you choose both modes, they happen in the listed order and nothing happens in between them. " +
                "Notably, if the second mode targets an Aura currently enchanting the target of the first mode, " +
                "that Aura will be returned to its owner's hand. (It wouldn't be put into a graveyard as a " +
                "state-based action until after Run Ashore is done resolving.)"
        )
    }
}
