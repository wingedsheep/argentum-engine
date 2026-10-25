package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.times
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.SetBasePowerToughnessDynamicStatic
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Mordenkainen — {4}{U}{U} Legendary Planeswalker — Mordenkainen, loyalty 5.
 *
 * The −2 Dog Illusion's P/T is a characteristic-defining ability — a layer-7b static on the token
 * re-read every projection — so it grows and shrinks with its controller's hand.
 *
 * The −10 gathers both zones before moving either, so the exchange is simultaneous: the old library
 * goes to hand (not drawn, per the ruling) and the old hand goes to the library, which is then
 * shuffled. The emblem is created for the player's command-zone badge; its effect is the
 * rest-of-game player-scoped "no maximum hand size" (an emblem can never leave, so the two are the
 * same thing), which is what `MaximumHandSize` reads.
 */
val Mordenkainen = card("Mordenkainen") {
    manaCost = "{4}{U}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Planeswalker — Mordenkainen"
    startingLoyalty = 5
    oracleText = "+2: Draw two cards, then put a card from your hand on the bottom of your library.\n" +
        "−2: Create a blue Dog Illusion creature token with \"This token's power and toughness are " +
        "each equal to twice the number of cards in your hand.\"\n" +
        "−10: Exchange your hand and library, then shuffle. You get an emblem with \"You have no " +
        "maximum hand size.\""

    loyaltyAbility(+2) {
        effect = Effects.DrawCards(2) then Effects.Pipeline {
            val handCards = gather(CardSource.FromZone(Zone.HAND, Player.You))
            val chosen = chooseExactly(
                1,
                from = handCards,
                prompt = "Put a card from your hand on the bottom of your library"
            )
            toLibraryBottom(chosen, order = CardOrder.Preserve)
        }
    }

    loyaltyAbility(-2) {
        val twiceHand = DynamicAmounts.cardsInYourHand() * 2
        effect = Effects.CreateToken(
            power = 0,
            toughness = 0,
            colors = setOf(Color.BLUE),
            creatureTypes = setOf("Dog", "Illusion"),
            staticAbilities = listOf(
                SetBasePowerToughnessDynamicStatic(
                    power = twiceHand,
                    toughness = twiceHand,
                    filter = GroupFilter.source(),
                )
            ),
            imageUri = "https://cards.scryfall.io/normal/front/c/e/ce3c0bd9-8a37-4164-9937-f35d1c210fe8.jpg?1783926346"
        )
    }

    loyaltyAbility(-10) {
        effect = Effects.Pipeline(
            descriptionOverride = "Exchange your hand and library, then shuffle. You get an emblem " +
                "with \"You have no maximum hand size.\""
        ) {
            val oldHand = gather(CardSource.FromZone(Zone.HAND, Player.You))
            val oldLibrary = gather(CardSource.FromZone(Zone.LIBRARY, Player.You))
            toHand(oldLibrary)
            toLibraryBottom(oldHand, order = CardOrder.Preserve)
            run(Effects.ShuffleLibrary())
            run(Effects.CreatePermanentEmblem(emblemDescription = "You have no maximum hand size."))
            run(Effects.RemoveMaximumHandSize())
        }
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "64"
        artist = "Ryan Pancoast"
        imageUri = "https://cards.scryfall.io/normal/front/2/f/2fa5e4c7-12fa-4ec4-bd4b-752e367306c7.jpg?1783926511"
        ruling(
            "2021-07-23",
            "The power and toughness of the Dog Illusion token will change as the number of cards in your hand changes."
        )
        ruling(
            "2021-07-23",
            "The cards put into your hand due to Mordenkainen's last ability aren't \"drawn.\" Anything that cares about you drawing cards won't notice your hand suddenly becoming very, very well-stocked."
        )
    }
}
