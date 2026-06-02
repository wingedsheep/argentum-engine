package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.dsl.Effects

/**
 * United Battlefront
 * {3}{W}
 * Sorcery
 * Look at the top seven cards of your library. Put up to two noncreature, nonland
 * permanent cards with mana value 3 or less from among them onto the battlefield.
 * Put the rest on the bottom of your library in a random order.
 *
 * Gather (top 7) → Select (up to 2 matching) → Move kept to battlefield, rest to the
 * bottom in a random order. Per the printed ruling, {X} in a card's mana cost counts
 * as 0 for mana value, which `CardPredicate.ManaValueAtMost` already honors.
 */
val UnitedBattlefront = card("United Battlefront") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Look at the top seven cards of your library. Put up to two noncreature, nonland " +
        "permanent cards with mana value 3 or less from among them onto the battlefield. " +
        "Put the rest on the bottom of your library in a random order."

    spell {
        effect = Effects.Composite(
            listOf(
                GatherCardsEffect(
                    source = CardSource.TopOfLibrary(DynamicAmount.Fixed(7)),
                    storeAs = "looked"
                ),
                SelectFromCollectionEffect(
                    from = "looked",
                    selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(2)),
                    filter = GameObjectFilter(
                        cardPredicates = listOf(
                            CardPredicate.IsNoncreature,
                            CardPredicate.IsNonland,
                            CardPredicate.IsPermanent,
                            CardPredicate.ManaValueAtMost(3)
                        )
                    ),
                    storeSelected = "onto",
                    storeRemainder = "rest",
                    selectedLabel = "Put onto the battlefield",
                    remainderLabel = "Put on bottom"
                ),
                MoveCollectionEffect(
                    from = "onto",
                    destination = CardDestination.ToZone(Zone.BATTLEFIELD)
                ),
                MoveCollectionEffect(
                    from = "rest",
                    destination = CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Bottom),
                    order = CardOrder.Random
                )
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "32"
        artist = "Darren Tan"
        imageUri = "https://cards.scryfall.io/normal/front/d/f/dff398be-4ba4-4976-9acc-be99d2e07a61.jpg?1743204090"
    }
}
