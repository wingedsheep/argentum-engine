package com.wingedsheep.mtg.sets.definitions.m15.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder

/**
 * Jalira, Master Polymorphist
 * {3}{U}
 * Legendary Creature — Human Wizard
 * 2/2
 * {2}{U}, {T}, Sacrifice another creature: Reveal cards from the top of your library until you
 * reveal a nonlegendary creature card. Put that card onto the battlefield and the rest on the
 * bottom of your library in a random order.
 *
 * Brion Stoutarm's cost shape ([Costs.SacrificeAnother] keeps Jalira herself out of the
 * sacrifice) over The Prismatic Bridge's reveal: gather until a nonlegendary creature card, put it
 * onto the battlefield, every other revealed card to the bottom at random. With no nonlegendary
 * creature card in the library, the whole library is revealed and goes back in a random order
 * (ruling).
 */
val JaliraMasterPolymorphist = card("Jalira, Master Polymorphist") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Human Wizard"
    power = 2
    toughness = 2
    oracleText = "{2}{U}, {T}, Sacrifice another creature: Reveal cards from the top of your library " +
        "until you reveal a nonlegendary creature card. Put that card onto the battlefield and the rest " +
        "on the bottom of your library in a random order."

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{2}{U}"),
            Costs.Tap,
            Costs.SacrificeAnother(GameObjectFilter.Creature),
        )
        effect = Effects.Pipeline {
            val (found, allRevealed) = gatherUntilMatch(GameObjectFilter.Creature.nonlegendary())
            // Zone-transition reveal so the client shows the whole pile, including the creature
            // card that lands on the battlefield in the same update.
            reveal(allRevealed, fromZone = Zone.LIBRARY, toZone = Zone.BATTLEFIELD)
            move(found, CardDestination.ToZone(Zone.BATTLEFIELD))
            toLibraryBottom(exclude(allRevealed, found), order = CardOrder.Random)
        }
        description = "Reveal cards from the top of your library until you reveal a nonlegendary " +
            "creature card. Put that card onto the battlefield and the rest on the bottom of your " +
            "library in a random order."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "64"
        artist = "Steve Prescott"
        flavorText = "\"You can become anything if I just put my mind to it.\""
        imageUri = "https://cards.scryfall.io/normal/front/4/2/425da4aa-4374-41b0-a665-568a04f57c38.jpg?1783939191"
        ruling(
            "2018-03-16",
            "If your library has no nonlegendary creature cards in it, you'll reveal all the cards in your " +
                "library, then put them in a random order. Although this is not technically shuffling your " +
                "library, no player is allowed to know the order of those cards and you will randomize the " +
                "cards in your deck."
        )
    }
}
