package com.wingedsheep.mtg.sets.definitions.tmt.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Cowabunga!
 * {G}
 * Sorcery
 *
 * Look at the top four cards of your library. You may reveal a Mutant,
 * Ninja, Turtle, or land card from among them and put it into your hand.
 * Put the rest on the bottom of your library in a random order.
 */
val Cowabunga = card("Cowabunga!") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Look at the top four cards of your library. You may reveal a Mutant, Ninja, Turtle, or land card from among them and put it into your hand. Put the rest on the bottom of your library in a random order."

    spell {
        effect = Effects.Pipeline {
            val looked = gather(
                CardSource.TopOfLibrary(DynamicAmount.Fixed(4)),
                name = "looked"
            )
            val (kept, rest) = chooseUpToSplit(
                1, from = looked,
                filter = GameObjectFilter(
                    cardPredicates = listOf(
                        CardPredicate.Or(
                            listOf(
                                CardPredicate.HasAnyOfSubtypes(
                                    listOf(Subtype("Mutant"), Subtype("Ninja"), Subtype("Turtle"))
                                ),
                                CardPredicate.IsLand,
                            )
                        )
                    )
                ),
                selectedLabel = "Put in hand",
                remainderLabel = "Put on bottom",
                showAllCards = true,
                name = "kept",
                remainderName = "rest"
            )
            move(
                kept,
                CardDestination.ToZone(Zone.HAND),
                revealed = true
            )
            move(
                rest,
                CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Bottom),
                order = CardOrder.Random,
            )
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "113"
        artist = "Thomas Chamberlain-Keen"
        flavorText = "When you're open to new things, a weird world of wonder unfolds before you."
        imageUri = "https://cards.scryfall.io/normal/front/0/f/0ffce972-bed9-445a-a9a0-4816f156d88d.jpg?1771342415"
    }
}
