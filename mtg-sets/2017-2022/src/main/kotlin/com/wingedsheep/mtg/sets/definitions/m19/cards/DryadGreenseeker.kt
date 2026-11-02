package com.wingedsheep.mtg.sets.definitions.m19.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource

/**
 * Dryad Greenseeker — Core Set 2019 #178 (canonical printing)
 * {1}{G}
 * Creature — Dryad
 * 1/3
 * {T}: Look at the top card of your library. If it's a land card, you may reveal it and put it
 * into your hand.
 *
 * The look-at-top peek pipeline (as Traveling Botanist): peek the top card privately, narrow it
 * to a land, and let the player choose up to one to reveal and put into hand. A non-land or
 * declined card is never moved, so it stays on top of the library.
 */
val DryadGreenseeker = card("Dryad Greenseeker") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Dryad"
    power = 1
    toughness = 3
    oracleText = "{T}: Look at the top card of your library. If it's a land card, you may reveal it " +
        "and put it into your hand."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.Pipeline {
            val looked = gather(CardSource.TopOfLibrary(1))
            val landCard = filter(looked, GameObjectFilter.Land)
            val toHand = chooseUpTo(
                1,
                from = landCard,
                selectedLabel = "Reveal and put into your hand",
                remainderLabel = "Leave on top of your library"
            )
            move(toHand, CardDestination.ToZone(Zone.HAND), revealed = true, revealToSelf = false)
        }
        description = "{T}: Look at the top card of your library. If it's a land card, you may reveal it " +
            "and put it into your hand."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "178"
        artist = "Greg Opalinski"
        flavorText = "\"The land already knows its destiny. I simply ask it to share the tale with me.\""
        imageUri = "https://cards.scryfall.io/normal/front/3/0/30171803-1e7b-430b-a0de-2dfd2f1115ac.jpg?1783934537"
        ruling(
            "2018-07-13",
            "If the top card of your library isn't a land card, or if you choose not to reveal it, it " +
                "remains on top of your library."
        )
    }
}
