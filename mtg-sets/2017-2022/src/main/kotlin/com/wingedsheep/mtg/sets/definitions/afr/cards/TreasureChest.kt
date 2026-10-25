package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.EmitLibrarySearchedEventEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Treasure Chest
 * {3}
 * Artifact
 * {4}, Sacrifice this artifact: Roll a d20.
 * 1 | Trapped! — You lose 3 life.
 * 2—9 | Create five Treasure tokens.
 * 10—19 | You gain 3 life and draw three cards.
 * 20 | Search your library for a card. If it's an artifact card, you may put it onto the
 * battlefield. Otherwise, put that card into your hand. Then shuffle.
 *
 * On a 20 the found card is split by type: a nonartifact goes straight to hand, an artifact asks
 * "put it onto the battlefield?" and goes to hand on a decline — per the ruling, "otherwise" refers
 * to not putting it onto the battlefield. The card is not revealed on its way to hand.
 */
val TreasureChest = card("Treasure Chest") {
    manaCost = "{3}"
    typeLine = "Artifact"
    oracleText = "{4}, Sacrifice this artifact: Roll a d20.\n" +
        "1 | Trapped! — You lose 3 life.\n" +
        "2—9 | Create five Treasure tokens.\n" +
        "10—19 | You gain 3 life and draw three cards.\n" +
        "20 | Search your library for a card. If it's an artifact card, you may put it onto the battlefield. " +
        "Otherwise, put that card into your hand. Then shuffle."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{4}"), Costs.SacrificeSelf)
        description = "Roll a d20. 1: you lose 3 life. 2—9: create five Treasure tokens. 10—19: you gain 3 " +
            "life and draw three cards. 20: search your library for a card; if it's an artifact card, you may " +
            "put it onto the battlefield, otherwise put it into your hand; then shuffle."
        effect = Effects.Pipeline {
            val result = rollDiceResult(sides = 20)
            fun between(low: Int, high: Int) = Conditions.All(
                Conditions.CompareAmounts(result.amount, ComparisonOperator.GTE, low),
                Conditions.CompareAmounts(result.amount, ComparisonOperator.LTE, high),
            )
            ifThen(Conditions.CompareAmounts(result.amount, ComparisonOperator.EQ, 1)) {
                run(Effects.LoseLife(3, EffectTarget.Controller))
            }
            ifThen(between(2, 9)) {
                run(Effects.CreateTreasure(5))
            }
            ifThen(between(10, 19)) {
                run(Effects.GainLife(3))
                run(Effects.DrawCards(3))
            }
            ifThen(Conditions.CompareAmounts(result.amount, ComparisonOperator.EQ, 20)) {
                val library = gather(CardSource.FromZone(Zone.LIBRARY), search = true)
                // An unrestricted search must find a card when the library is nonempty.
                val found = chooseExactly(1, from = library, prompt = "Choose a card")
                val (artifact, other) = filterSplit(found, GameObjectFilter.Artifact)
                toHand(other)
                ifNotEmpty(artifact) {
                    run(
                        Effects.May(
                            effect = Effects.Move(artifact.asTarget, Zone.BATTLEFIELD),
                            otherwise = Effects.Move(artifact.asTarget, Zone.HAND),
                            descriptionOverride = "Put the artifact card onto the battlefield? (Otherwise it goes to your hand.)"
                        )
                    )
                }
                run(Effects.ShuffleLibrary())
                run(EmitLibrarySearchedEventEffect)
            }
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "252"
        artist = "Dan Murayama Scott"
        imageUri = "https://cards.scryfall.io/normal/front/6/e/6ecbeb07-0f45-49bf-9a12-f4ca09969122.jpg?1783926436"
        ruling("2021-07-23", "The “otherwise” in the last ability refers to whether or not you put it onto the battlefield, not whether or not it's an artifact card. You may put it into your hand even if it's an artifact card.")
        ruling("2021-07-23", "If you choose to put the card into your hand, you don't need to reveal it.")
    }
}
