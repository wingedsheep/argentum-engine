package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.EmitLibrarySearchedEventEffect
import com.wingedsheep.sdk.scripting.effects.SelectionRestriction
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Gifts Ungiven — Champions of Kamigawa #62
 * {3}{U} · Instant
 *
 * Search your library for up to four cards with different names and reveal them. Target
 * opponent chooses two of those cards. Put the chosen cards into your graveyard and the rest
 * into your hand. Then shuffle.
 *
 * The Elemental Teachings pipeline with any card and a *targeted* opponent:
 *  1. Gather the library → controller `ChooseUpTo(4)` under `OnePerCardName` ("with different
 *     names"). "Up to four" lets the controller find fewer, or none.
 *  2. Reveal what was found.
 *  3. The target opponent (`Chooser.TargetPlayer`) chooses exactly two → graveyard; the
 *     remainder → hand. The executor clamps the count to the collection, so per the 2017-03-14
 *     ruling, finding one or two cards means all of them go to the graveyard.
 *  4. Shuffle; the search happened whether or not anything was found.
 */
val GiftsUngiven = card("Gifts Ungiven") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Search your library for up to four cards with different names and reveal them. " +
        "Target opponent chooses two of those cards. Put the chosen cards into your graveyard and " +
        "the rest into your hand. Then shuffle."

    spell {
        target(Targets.Opponent)
        effect = Effects.Pipeline {
            val searchable = gather(
                CardSource.FromZone(Zone.LIBRARY, Player.You, GameObjectFilter.Any),
                search = true
            )
            val found = chooseUpTo(
                4,
                from = searchable,
                restrictions = listOf(SelectionRestriction.OnePerCardName),
                prompt = "Search for up to four cards with different names"
            )
            reveal(found)
            val (toGraveyardCards, toHandCards) = chooseExactlySplit(
                2,
                from = found,
                chooser = Chooser.TargetPlayer,
                selectedLabel = "Graveyard",
                remainderLabel = "Hand",
                prompt = "Choose two of the revealed cards. Those go into their owner's graveyard; " +
                    "the rest go into their hand."
            )
            toGraveyard(toGraveyardCards)
            toHand(toHandCards)
            run(Effects.ShuffleLibrary())
            // CR 701.23 — the search happened whether or not anything was found.
            run(EmitLibrarySearchedEventEffect)
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "62"
        artist = "D. Alexander Gregory"
        imageUri = "https://cards.scryfall.io/normal/front/3/2/32b91eb5-ea53-4a21-a2e5-9c545a42fa30.jpg?1783944328"
        ruling(
            "2017-03-14",
            "You can choose to find fewer than four cards if you want. If you find one or two cards, " +
                "your opponent must choose for them to be put into your graveyard, even if they don't want to."
        )
    }
}
