package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * You Happen On a Glade
 * {2}{G}
 * Instant
 *
 * Choose one —
 * • Journey On — Search your library for up to two basic land cards, reveal them, put them into
 *   your hand, then shuffle.
 * • Make Camp — Return target permanent card from your graveyard to your hand.
 *
 * A choose-one `modal(chooseCount = 1)`: the Journey of Discovery basic-land search to hand, and
 * the Nature's Spiral permanent-card regrowth.
 */
val YouHappenOnAGlade = card("You Happen On a Glade") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Choose one —\n" +
        "• Journey On — Search your library for up to two basic land cards, reveal them, put them into your hand, then shuffle.\n" +
        "• Make Camp — Return target permanent card from your graveyard to your hand."

    spell {
        modal(chooseCount = 1) {
            mode("Journey On — Search your library for up to two basic land cards, reveal them, put them into your hand, then shuffle.") {
                effect = Patterns.Library.searchLibrary(
                    filter = GameObjectFilter.BasicLand,
                    count = 2,
                    destination = SearchDestination.HAND,
                    shuffleAfter = true,
                    reveal = true
                )
            }
            mode("Make Camp — Return target permanent card from your graveyard to your hand.") {
                val permanentCard = target(TargetFilter(GameObjectFilter.Permanent.ownedByYou(), zone = Zone.GRAVEYARD))
                effect = Effects.Move(target = permanentCard, destination = Zone.HAND)
            }
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "214"
        artist = "Anna Fehr"
        imageUri = "https://cards.scryfall.io/normal/front/f/9/f9ae7693-a6b5-42f8-9f05-c0643b95a710.jpg?1783926450"
    }
}
