package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Supertype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.EnteringAttackDefender
import com.wingedsheep.sdk.scripting.effects.RepeatCondition
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Delina, Wild Mage
 * {3}{R}
 * Legendary Creature — Elf Shaman
 * 3/2
 * Whenever Delina attacks, choose target creature you control, then roll a d20.
 * 1—14 | Create a tapped and attacking token that's a copy of that creature, except it's not
 * legendary and it has "At end of combat, exile this token."
 * 15—20 | Create one of those tokens. You may roll again.
 *
 * Each roll is one pass of a loop whose "you may roll again" is offered only after a 15—20 pass
 * (`RepeatCondition.PlayerChooses.onlyIf`). The creature is targeted once, so every pass copies the
 * same creature. Each token's controller picks what it attacks as it enters (CR 508.4); it was never
 * declared as an attacker.
 */
val DelinaWildMage = card("Delina, Wild Mage") {
    manaCost = "{3}{R}"
    typeLine = "Legendary Creature — Elf Shaman"
    power = 3
    toughness = 2
    oracleText = "Whenever Delina attacks, choose target creature you control, then roll a d20.\n" +
        "1—14 | Create a tapped and attacking token that's a copy of that creature, except it's not legendary and it has \"At end of combat, exile this token.\"\n" +
        "15—20 | Create one of those tokens. You may roll again."

    triggeredAbility {
        trigger = Triggers.self.attacks()
        val creature = target(TargetFilter(GameObjectFilter.Creature.youControl()))
        val token = Effects.CreateTokenCopyOfTarget(
            target = creature,
            tapped = true,
            attacking = true,
            attackDefender = EnteringAttackDefender.CONTROLLER_CHOOSES,
            removedSupertypes = setOf(Supertype.LEGENDARY),
            triggeredAbilities = listOf(
                TriggeredAbility.create(
                    trigger = Triggers.anyPlayer.beginningOf(Step.END_COMBAT),
                    effect = Effects.Exile(EffectTarget.Self),
                    descriptionOverride = "At end of combat, exile this token.",
                )
            ),
        )
        effect = Effects.Pipeline {
            repeatProcess {
                val result = rollDiceResult(sides = 20)
                fun between(low: Int, high: Int) = Conditions.All(
                    Conditions.CompareAmounts(result.amount, ComparisonOperator.GTE, low),
                    Conditions.CompareAmounts(result.amount, ComparisonOperator.LTE, high),
                )
                ifThen(between(1, 14)) { run(token) }
                ifThen(between(15, 20)) { run(token) }
                RepeatCondition.PlayerChooses(
                    decider = EffectTarget.Controller,
                    prompt = "Roll the d20 again?",
                    yesText = "Roll again",
                    noText = "Stop",
                    onlyIf = between(15, 20),
                )
            }
        }
        description = "Whenever Delina attacks, choose target creature you control, then roll a d20. " +
            "1—14: create a tapped and attacking token that's a copy of that creature, except it's not " +
            "legendary and it has \"At end of combat, exile this token.\" 15—20: create one of those " +
            "tokens. You may roll again."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "138"
        artist = "Wisnu Tan"
        imageUri = "https://cards.scryfall.io/normal/front/e/8/e87459aa-af8f-4bd2-a310-151353083a2e.jpg?1783926480"
        ruling("2021-07-23", "Delina, Wild Mage has received an update to its Oracle text. Specifically, rolling again on a result of 15–20 is optional.")
        ruling("2021-07-23", "You declare which player or planeswalker the token is attacking as you put it onto the battlefield. It doesn't have to be the same player or planeswalker Delina, Wild Mage is attacking.")
        ruling("2021-07-23", "Although the token is attacking, it was never declared as an attacking creature (for purposes of abilities that trigger whenever a creature attacks, for example).")
    }
}
