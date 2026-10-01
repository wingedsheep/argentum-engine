package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.plus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Green Sun's Twilight
 * {X}{G}
 * Sorcery
 * Reveal the top X plus one cards of your library. Choose a creature card and/or a land card from
 * among them. Put those cards into your hand and the rest on the bottom of your library in a random
 * order. If X is 5 or more, instead put the chosen cards onto the battlefield or into your hand and
 * the rest on the bottom of your library in a random order.
 *
 * "And/or" is two chained up-to-one picks (the In the Presence of Ages / Ojer Kaslem shape): a
 * creature, then a land from what's left, so one card can't be taken twice. With X >= 5 the
 * controller makes a single choice for both cards (2023-02-04 ruling) — a `May` whose decline
 * branch puts them into the hand. The choice is skipped when nothing was chosen.
 */
val GreenSunsTwilight = card("Green Sun's Twilight") {
    manaCost = "{X}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Reveal the top X plus one cards of your library. Choose a creature card and/or a land card from among them. Put those cards into your hand and the rest on the bottom of your library in a random order. If X is 5 or more, instead put the chosen cards onto the battlefield or into your hand and the rest on the bottom of your library in a random order."

    spell {
        effect = Effects.Pipeline {
            val revealed = gather(
                CardSource.TopOfLibrary(DynamicAmounts.xValue() + 1, Player.You),
                revealed = true
            )
            val (creature, afterCreature) = chooseUpToSplit(
                1,
                from = revealed,
                filter = GameObjectFilter.Creature,
                showAllCards = true,
                prompt = "Choose a creature card"
            )
            val (land, rest) = chooseUpToSplit(
                1,
                from = afterCreature,
                filter = GameObjectFilter.Land,
                showAllCards = true,
                prompt = "Choose a land card",
                remainderLabel = "Put on the bottom of your library"
            )
            val intoHand = Effects.Pipeline {
                toHand(creature, revealed = true)
                toHand(land, revealed = true)
            }
            run(
                Effects.If(
                    condition = Conditions.All(
                        Conditions.CompareAmounts(DynamicAmounts.xValue(), ComparisonOperator.GTE, 5),
                        Conditions.Any(whenMatches(creature), whenMatches(land))
                    ),
                    then = Effects.May(
                        Effects.Pipeline {
                            move(creature, CardDestination.ToZone(Zone.BATTLEFIELD, Player.You))
                            move(land, CardDestination.ToZone(Zone.BATTLEFIELD, Player.You))
                        },
                        otherwise = intoHand,
                        prompt = "Put the chosen cards onto the battlefield? (Otherwise, into your hand.)"
                    ),
                    otherwise = intoHand
                )
            )
            toLibraryBottom(rest, order = CardOrder.Random)
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "169"
        artist = "Yeong-Hao Han"
        imageUri = "https://cards.scryfall.io/normal/front/1/8/184c985d-cb70-41a7-9f66-3506708b7e26.jpg?1783918017"
        ruling("2023-02-04", "If X is 5 or more, you choose whether to put both cards into your hand or both cards onto the battlefield. You can't choose to put one into your hand and the other onto the battlefield.")
    }
}
