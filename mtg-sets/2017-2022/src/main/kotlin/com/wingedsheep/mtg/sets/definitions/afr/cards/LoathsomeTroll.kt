package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Loathsome Troll
 * {3}{G}{G}
 * Creature — Troll
 * 6/2
 * {3}{G}: Roll a d20. Activate only if this card is in your graveyard.
 * 1—9 | Put this card on top of your library.
 * 10—19 | Return this card to your hand.
 * 20 | Return this card to the battlefield tapped.
 *
 * Every branch moves the card with `fromZone = Zone.GRAVEYARD`, so if the card has left the
 * graveyard before the ability resolves, the roll still happens but the card stays put.
 */
val LoathsomeTroll = card("Loathsome Troll") {
    manaCost = "{3}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Troll"
    power = 6
    toughness = 2
    oracleText = "{3}{G}: Roll a d20. Activate only if this card is in your graveyard.\n" +
        "1—9 | Put this card on top of your library.\n" +
        "10—19 | Return this card to your hand.\n" +
        "20 | Return this card to the battlefield tapped."

    activatedAbility {
        cost = Costs.Mana("{3}{G}")
        activateFromZone = Zone.GRAVEYARD
        effect = Effects.Pipeline {
            val result = rollDiceResult(sides = 20)
            fun between(low: Int, high: Int) = Conditions.All(
                Conditions.CompareAmounts(result.amount, ComparisonOperator.GTE, low),
                Conditions.CompareAmounts(result.amount, ComparisonOperator.LTE, high),
            )
            ifThen(between(1, 9)) {
                run(Effects.Move(EffectTarget.Self, Zone.LIBRARY, ZonePlacement.Top, fromZone = Zone.GRAVEYARD))
            }
            ifThen(between(10, 19)) {
                run(Effects.Move(EffectTarget.Self, Zone.HAND, fromZone = Zone.GRAVEYARD))
            }
            ifThen(Conditions.CompareAmounts(result.amount, ComparisonOperator.GTE, 20)) {
                run(Effects.Move(EffectTarget.Self, Zone.BATTLEFIELD, ZonePlacement.Tapped, fromZone = Zone.GRAVEYARD))
            }
        }
        description = "Roll a d20. 1—9: put this card on top of your library. 10—19: return it to your hand. " +
            "20: return it to the battlefield tapped."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "192"
        artist = "Svetlin Velinov"
        imageUri = "https://cards.scryfall.io/normal/front/5/0/50ac66df-760d-495d-a067-13114071b7b8.jpg?1783926460"
    }
}
