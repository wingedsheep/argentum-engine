package com.wingedsheep.mtg.sets.definitions.dominaria.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CollectionFilter
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.FilterCollectionEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.values.DynamicAmount

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
    typeLine = "Legendary Sorcery"
    oracleText = "(You may cast a legendary sorcery only if you control a legendary creature or planeswalker.)\nLook at the top X cards of your library. You may put any number of land and/or legendary permanent cards with mana value X or less from among them onto the battlefield. Put the rest into your graveyard."

    spell {
        castOnlyIf(Conditions.ControlLegendaryCreatureOrPlaneswalker)
        effect = CompositeEffect(
            listOf(
                // Look at the top X cards
                GatherCardsEffect(
                    source = CardSource.TopOfLibrary(DynamicAmount.XValue),
                    storeAs = "looked"
                ),
                // Filter to cards with mana value X or less
                FilterCollectionEffect(
                    from = "looked",
                    filter = CollectionFilter.ManaValueAtMost(DynamicAmount.XValue),
                    storeMatching = "mvOk",
                    storeNonMatching = "mvTooHigh"
                ),
                // Player selects any number of land and/or legendary permanent cards
                SelectFromCollectionEffect(
                    from = "mvOk",
                    selection = SelectionMode.ChooseAnyNumber,
                    filter = LandOrLegendaryPermanent,
                    storeSelected = "chosen",
                    storeRemainder = "unchosen",
                    prompt = "Put any number of land and/or legendary permanent cards onto the battlefield",
                    showAllCards = true
                ),
                // Put chosen cards onto the battlefield
                MoveCollectionEffect(
                    from = "chosen",
                    destination = CardDestination.ToZone(Zone.BATTLEFIELD)
                ),
                // Put unchosen cards into graveyard
                MoveCollectionEffect(
                    from = "unchosen",
                    destination = CardDestination.ToZone(Zone.GRAVEYARD)
                ),
                // Put cards with MV too high into graveyard
                MoveCollectionEffect(
                    from = "mvTooHigh",
                    destination = CardDestination.ToZone(Zone.GRAVEYARD)
                )
            )
        )
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
