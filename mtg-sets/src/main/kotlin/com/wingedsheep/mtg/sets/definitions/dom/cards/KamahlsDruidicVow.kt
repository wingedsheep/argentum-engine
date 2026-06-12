package com.wingedsheep.mtg.sets.definitions.dom.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CollectionFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.dsl.Effects

/**
 * Filter for land and/or legendary permanent cards.
 * Matches: any land card, or any permanent card that is legendary.
 */
private val LandOrLegendaryPermanent = GameObjectFilter(
    cardPredicates = listOf(
        CardPredicate.Or(
            listOf(
                CardPredicate.IsLand,
                CardPredicate.And(listOf(CardPredicate.IsPermanent, CardPredicate.IsLegendary))
            )
        )
    )
)

/**
 * Kamahl's Druidic Vow
 * {X}{G}{G}
 * Legendary Sorcery
 *
 * (You may cast a legendary sorcery only if you control a legendary creature or planeswalker.)
 * Look at the top X cards of your library. You may put any number of land and/or legendary
 * permanent cards with mana value X or less from among them onto the battlefield. Put the rest
 * into your graveyard.
 */
val KamahlsDruidicVow = card("Kamahl's Druidic Vow") {
    manaCost = "{X}{G}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Sorcery"
    oracleText = "(You may cast a legendary sorcery only if you control a legendary creature or planeswalker.)\nLook at the top X cards of your library. You may put any number of land and/or legendary permanent cards with mana value X or less from among them onto the battlefield. Put the rest into your graveyard."

    spell {
        castOnlyIf(Conditions.ControlLegendaryCreatureOrPlaneswalker)
        effect = Effects.Pipeline {
            // Look at the top X cards
            val looked = gather(
                CardSource.TopOfLibrary(DynamicAmount.XValue),
                name = "looked"
            )
            // Filter to cards with mana value X or less
            val (mvOk, mvTooHigh) = filterSplit(
                looked,
                CollectionFilter.ManaValueAtMost(DynamicAmount.XValue),
                name = "mvOk",
                restName = "mvTooHigh"
            )
            // Player selects any number of land and/or legendary permanent cards
            val (chosen, unchosen) = chooseAnyNumberSplit(
                from = mvOk,
                filter = LandOrLegendaryPermanent,
                prompt = "Put any number of land and/or legendary permanent cards onto the battlefield",
                showAllCards = true,
                name = "chosen",
                remainderName = "unchosen"
            )
            // Put chosen cards onto the battlefield
            move(
                chosen,
                destination = CardDestination.ToZone(Zone.BATTLEFIELD)
            )
            // Put unchosen cards into graveyard
            move(
                unchosen,
                destination = CardDestination.ToZone(Zone.GRAVEYARD)
            )
            // Put cards with MV too high into graveyard
            move(
                mvTooHigh,
                destination = CardDestination.ToZone(Zone.GRAVEYARD)
            )
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "166"
        artist = "Noah Bradley"
        flavorText = "\"Centuries ago, a barbarian laid his rage to rest.\""
        imageUri = "https://cards.scryfall.io/normal/front/b/f/bf95ef60-e67b-466d-a6cb-d487ffa88b72.jpg?1562742183"
        ruling("2018-04-27", "For cards in your library with {X} in their mana costs, X is considered to be 0.")
        ruling("2018-04-27", "All of the permanents put onto the battlefield this way enter at the same time. If any have triggered abilities that trigger on something else entering the battlefield, they'll see each other.")
    }
}
