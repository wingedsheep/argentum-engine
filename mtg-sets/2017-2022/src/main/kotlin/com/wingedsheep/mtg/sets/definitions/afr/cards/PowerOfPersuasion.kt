package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Power of Persuasion
 * {2}{U}
 * Sorcery
 * Choose target creature an opponent controls, then roll a d20.
 * 1—9 | Return it to its owner's hand.
 * 10—19 | Its owner puts it on their choice of the top or bottom of their library.
 * 20 | Gain control of it until the end of your next turn.
 *
 * The target is chosen on cast; the d20 is rolled on resolution. In the 10—19 row the creature's
 * owner (not the caster) picks the library end — [Effects.PutOnTopOrBottomOfLibrary] asks the owner.
 */
val PowerOfPersuasion = card("Power of Persuasion") {
    manaCost = "{2}{U}"
    typeLine = "Sorcery"
    oracleText = "Choose target creature an opponent controls, then roll a d20.\n" +
        "1—9 | Return it to its owner's hand.\n" +
        "10—19 | Its owner puts it on their choice of the top or bottom of their library.\n" +
        "20 | Gain control of it until the end of your next turn."

    spell {
        val creature = target(TargetFilter.CreatureOpponentControls)
        effect = Effects.Pipeline {
            val result = rollDiceResult(sides = 20)
            fun between(low: Int, high: Int) = Conditions.All(
                Conditions.CompareAmounts(result.amount, ComparisonOperator.GTE, low),
                Conditions.CompareAmounts(result.amount, ComparisonOperator.LTE, high),
            )
            ifThen(between(1, 9)) {
                run(Effects.ReturnToHand(creature))
            }
            ifThen(between(10, 19)) {
                run(Effects.PutOnTopOrBottomOfLibrary(creature))
            }
            ifThen(Conditions.CompareAmounts(result.amount, ComparisonOperator.GTE, 20)) {
                run(Effects.GainControl(creature, Duration.EndOfYourNextTurn))
            }
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "67"
        artist = "Brian Valeza"
        imageUri = "https://cards.scryfall.io/normal/front/8/d/8d84cb72-d0a0-4af0-aded-47e5fb7addef.jpg?1783926511"
        ruling("2021-07-23", "The instruction to roll a die and the effect that occurs because of the result are all part of the same ability. Players do not get the chance to respond to the ability after knowing the result of the roll.")
        ruling("2021-07-23", "An effect that says \"choose a target, then roll a d20\" or similar still uses the normal process of putting an ability on the stack and resolving it. Choosing targets is part of putting the ability on the stack and rolling the d20 happens later, as the ability resolves.")
    }
}
