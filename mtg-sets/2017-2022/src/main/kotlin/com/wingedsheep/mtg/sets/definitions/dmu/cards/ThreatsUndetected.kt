package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.EmitLibrarySearchedEventEffect
import com.wingedsheep.sdk.scripting.effects.SelectionRestriction
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Threats Undetected — Dominaria United #185
 * {2}{G} · Sorcery
 *
 * Search your library for up to four creature cards with different powers and reveal them. An
 * opponent chooses two of those cards. Shuffle the chosen cards into your library and put the
 * rest into your hand.
 *
 * The Gifts Ungiven pipeline with a creature filter and `OnePerPower` ("with different powers"),
 * an untargeted opponent (`Chooser.Opponent`) and a shuffle-into-library destination:
 *  1. Gather creature cards in the library → controller `ChooseUpTo(4)` under `OnePerPower`.
 *  2. Reveal what was found.
 *  3. The opponent chooses exactly two (clamped to the collection, so one or two found cards are
 *     all chosen) → shuffled into the library; the rest → hand.
 *  4. Shuffle explicitly too, so the library is shuffled even when nothing was found.
 */
val ThreatsUndetected = card("Threats Undetected") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Search your library for up to four creature cards with different powers and reveal " +
        "them. An opponent chooses two of those cards. Shuffle the chosen cards into your library and " +
        "put the rest into your hand."

    spell {
        effect = Effects.Pipeline {
            val searchable = gather(
                CardSource.FromZone(Zone.LIBRARY, Player.You, GameObjectFilter.Creature),
                search = true
            )
            val found = chooseUpTo(
                4,
                from = searchable,
                restrictions = listOf(SelectionRestriction.OnePerPower),
                prompt = "Search for up to four creature cards with different powers"
            )
            reveal(found)
            val (toLibraryCards, toHandCards) = chooseExactlySplit(
                2,
                from = found,
                chooser = Chooser.Opponent,
                selectedLabel = "Shuffle into library",
                remainderLabel = "Hand",
                prompt = "Choose two of the revealed cards. Those are shuffled into their owner's " +
                    "library; the rest go into their hand."
            )
            move(toLibraryCards, CardDestination.ToZone(Zone.LIBRARY, Player.You, ZonePlacement.Shuffled))
            toHand(toHandCards)
            run(Effects.ShuffleLibrary())
            // The search happened whether or not anything was found.
            run(EmitLibrarySearchedEventEffect)
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "185"
        artist = "Randy Vargas"
        imageUri = "https://cards.scryfall.io/normal/front/9/9/99545c3e-0710-48d3-8558-046bd66f225c.jpg?1783921293"
        ruling(
            "2022-09-09",
            "A creature card has different power from another if their powers are different numbers. " +
                "For example, a 1/1 creature and a 2/1 creature have different powers."
        )
    }
}
