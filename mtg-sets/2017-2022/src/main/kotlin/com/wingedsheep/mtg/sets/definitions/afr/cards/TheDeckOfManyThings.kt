package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.unaryMinus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerExpiry
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * The Deck of Many Things
 * {5}
 * Legendary Artifact
 * {2}, {T}: Roll a d20 and subtract the number of cards in your hand. If the result is 0 or less,
 * discard your hand.
 * 1—9 | Return a card at random from your graveyard to your hand.
 * 10—19 | Draw two cards.
 * 20 | Put a creature card from any graveyard onto the battlefield under your control. When that
 * creature dies, its owner loses the game.
 *
 * The hand size is a negative roll modifier, so the results table reads the modified result
 * (CR 706.2, 706.3a). "A creature card from any graveyard" is chosen on resolution, not targeted;
 * the "dies" delayed trigger watches that creature for as long as it stays on the battlefield and
 * makes its *owner* — who may be an opponent — lose the game.
 */
val TheDeckOfManyThings = card("The Deck of Many Things") {
    manaCost = "{5}"
    typeLine = "Legendary Artifact"
    oracleText = "{2}, {T}: Roll a d20 and subtract the number of cards in your hand. If the result is 0 or less, discard your hand.\n" +
        "1—9 | Return a card at random from your graveyard to your hand.\n" +
        "10—19 | Draw two cards.\n" +
        "20 | Put a creature card from any graveyard onto the battlefield under your control. When that creature dies, its owner loses the game."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.Tap)
        description = "Roll a d20 and subtract the number of cards in your hand. If the result is 0 or less, " +
            "discard your hand. 1—9: return a card at random from your graveyard to your hand. 10—19: draw two " +
            "cards. 20: put a creature card from any graveyard onto the battlefield under your control; when it " +
            "dies, its owner loses the game."
        effect = Effects.Pipeline {
            val result = rollDiceResult(
                sides = 20,
                modifier = -DynamicAmounts.cardsInYourHand(),
            )
            fun between(low: Int, high: Int) = Conditions.All(
                Conditions.CompareAmounts(result.amount, ComparisonOperator.GTE, low),
                Conditions.CompareAmounts(result.amount, ComparisonOperator.LTE, high),
            )
            ifThen(Conditions.CompareAmounts(result.amount, ComparisonOperator.LTE, 0)) {
                run(Patterns.Hand.discardHand())
            }
            ifThen(between(1, 9)) {
                val graveyard = gather(CardSource.FromZone(Zone.GRAVEYARD, Player.You))
                toHand(chooseRandom(1, from = graveyard))
            }
            ifThen(between(10, 19)) {
                run(Effects.DrawCards(2))
            }
            ifThen(Conditions.CompareAmounts(result.amount, ComparisonOperator.EQ, 20)) {
                val creatures = gather(CardSource.FromZone(Zone.GRAVEYARD, Player.Each, GameObjectFilter.Creature))
                val chosen = chooseExactly(1, from = creatures, prompt = "Put a creature card onto the battlefield under your control")
                move(chosen, CardDestination.ToZone(Zone.BATTLEFIELD, Player.You))
                run(
                    Effects.CreateDelayedTrigger(
                        trigger = Triggers.self.dies(),
                        watchedTarget = chosen.asTarget(0),
                        fireOnce = true,
                        expiry = DelayedTriggerExpiry.Never,
                        effect = Effects.LoseGame(EffectTarget.PlayerRef(Player.OwnerOfTriggeringEntity)),
                    )
                )
            }
        }
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "241"
        artist = "Volkan Baǵa"
        imageUri = "https://cards.scryfall.io/normal/front/f/e/feddbdc6-0757-43cb-bb41-dc83c6cf42ea.jpg?1783926439"
        ruling("2021-07-23", "Like all results tables, the one on The Deck of Many Things cares about the result of the die roll after any adjustments, such as subtracting the number of cards in your hand. This means it's usually not possible to get a result of 20 unless you have no cards in hand.")
        ruling("2021-07-23", "Some effects may modify the result of a die roll. This may be part of the instruction to roll a die or it may come from other cards. Anything that references the “result” of a die roll is looking for the result after these modifications. Anything that is looking for the “natural result” is looking for the number shown on the face of the die before these modifications.")
        ruling("2021-07-23", "Some abilities, like that of Pixie Guide and Barbarian Class, replace rolling a die with rolling extra dice and ignoring the lowest roll. The ignored rolls are not considered for the effect that instructed you to roll a die, and do not cause abilities to trigger. For all intents and purposes, once you determine which dice count, any extra dice were never rolled.")
    }
}
