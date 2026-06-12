package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ChooseActionEffect
import com.wingedsheep.sdk.scripting.effects.EffectChoice
import com.wingedsheep.sdk.scripting.effects.FeasibilityCheck
import com.wingedsheep.sdk.scripting.effects.MoveType
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Thirst for Discovery
 * {2}{U}
 * Instant
 *
 * Draw three cards. Then discard two cards unless you discard a basic land card.
 *
 * Modeled as: Draw 3, then ChooseAction between "discard a basic land card"
 * (feasible only if a basic land card is in hand) and "discard two cards".
 */
val ThirstForDiscovery = card("Thirst for Discovery") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Draw three cards. Then discard two cards unless you discard a basic land card."

    spell {
        effect = Effects.DrawCards(3)
            .then(
                ChooseActionEffect(
                    choices = listOf(
                        EffectChoice(
                            label = "Discard a basic land card",
                            effect = Effects.Pipeline {
                                val basicLands = gather(
                                    CardSource.FromZone(
                                        Zone.HAND,
                                        Player.You,
                                        GameObjectFilter.BasicLand
                                    ),
                                    name = "basicLands"
                                )
                                val discarded = chooseExactly(
                                    1, from = basicLands,
                                    prompt = "Choose a basic land card to discard",
                                    name = "discarded"
                                )
                                move(
                                    discarded,
                                    CardDestination.ToZone(Zone.GRAVEYARD),
                                    moveType = MoveType.Discard
                                )
                            },
                            feasibilityCheck = FeasibilityCheck.HasCardsInZone(
                                Zone.HAND,
                                GameObjectFilter.BasicLand,
                                1
                            )
                        ),
                        EffectChoice(
                            label = "Discard two cards",
                            effect = Patterns.Hand.discardCards(2)
                        )
                    )
                )
            )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "85"
        artist = "Dominik Mayer"
        flavorText = "\"This is your only warning, alchemist. The secrets of the sea are not yours to behold. Lord Krothuss will not be so merciful next time.\"\n—Runo Stromkirk"
        imageUri = "https://cards.scryfall.io/normal/front/1/e/1ea179e9-9c0d-46c1-9ee8-60be68e1f79c.jpg?1643588791"
    }
}
