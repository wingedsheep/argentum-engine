package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.FaceDownMode

/**
 * Write into Being
 * {2}{U}
 * Sorcery
 * Look at the top two cards of your library. Manifest one of those cards, then put the other on the
 * top or bottom of your library.
 *
 * The manifest-dread shape with a library tail instead of a graveyard one: the two cards are looked at
 * privately, the chosen one is manifested face down as a 2/2, and only then is the other put on top or
 * bottom (a scry-1 style "up to one" pick — selected goes to the bottom, the rest stays on top). With a
 * single card in library that card is manifested and there is nothing left to place.
 */
val WriteIntoBeing = card("Write into Being") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Look at the top two cards of your library. Manifest one of those cards, then put the other " +
        "on the top or bottom of your library. (To manifest a card, put it onto the battlefield face down as a " +
        "2/2 creature. Turn it face up any time for its mana cost if it's a creature card.)"

    spell {
        effect = Effects.Pipeline {
            val looked = gather(CardSource.TopOfLibrary(DynamicAmounts.fixed(2)))
            val (toManifest, other) = chooseExactlySplit(
                1,
                from = looked,
                prompt = "Choose a card to manifest",
                selectedLabel = "Manifest (put onto the battlefield face down)",
                remainderLabel = "Put on the top or bottom of your library"
            )
            move(toManifest, CardDestination.ToZone(Zone.BATTLEFIELD), faceDown = FaceDownMode.MANIFEST)
            val (toBottom, toTop) = chooseUpToSplit(
                1,
                from = other,
                prompt = "You may put the other card on the bottom of your library",
                selectedLabel = "Put on bottom",
                remainderLabel = "Put on top"
            )
            toLibraryBottom(toBottom, order = CardOrder.Preserve)
            toLibraryTop(toTop, order = CardOrder.Preserve)
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "59"
        artist = "Yeong-Hao Han"
        imageUri = "https://cards.scryfall.io/normal/front/a/8/a85e6fbe-cda0-4fbf-853c-c03c567a7de9.jpg?1783938701"
        ruling(
            "2014-11-24",
            "Other players won't know whether the card you manifest is the top card or second card of that library."
        )
        ruling(
            "2014-11-24",
            "If you're playing with the top card of your library revealed, you'll manifest one of the cards, then " +
                "the other one will be revealed, then you can choose to put that card on the bottom of your library " +
                "or leave it on top."
        )
    }
}
