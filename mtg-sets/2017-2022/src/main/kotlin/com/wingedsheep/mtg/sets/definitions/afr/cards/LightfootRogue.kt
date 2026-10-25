package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Lightfoot Rogue
 * {1}{B}
 * Creature — Halfling Rogue
 * 2/1
 * Sneak Attack — Whenever this creature attacks, roll a d20.
 * 1—9 | This creature gains deathtouch until end of turn.
 * 10—19 | It gets +1/+0 and gains deathtouch until end of turn.
 * 20 | It gets +3/+0 and gains first strike and deathtouch until end of turn.
 */
val LightfootRogue = card("Lightfoot Rogue") {
    manaCost = "{1}{B}"
    typeLine = "Creature — Halfling Rogue"
    power = 2
    toughness = 1
    oracleText = "Sneak Attack — Whenever this creature attacks, roll a d20.\n" +
        "1—9 | This creature gains deathtouch until end of turn.\n" +
        "10—19 | It gets +1/+0 and gains deathtouch until end of turn.\n" +
        "20 | It gets +3/+0 and gains first strike and deathtouch until end of turn."

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.Pipeline {
            val result = rollDiceResult(sides = 20)
            ifThen(Conditions.CompareAmounts(result.amount, ComparisonOperator.LTE, 9)) {
                run(Effects.GrantKeyword(Keyword.DEATHTOUCH, EffectTarget.Self))
            }
            ifThen(
                Conditions.All(
                    Conditions.CompareAmounts(result.amount, ComparisonOperator.GTE, 10),
                    Conditions.CompareAmounts(result.amount, ComparisonOperator.LTE, 19),
                )
            ) {
                run(
                    Effects.ModifyStats(1, 0, EffectTarget.Self) then
                        Effects.GrantKeyword(Keyword.DEATHTOUCH, EffectTarget.Self)
                )
            }
            ifThen(Conditions.CompareAmounts(result.amount, ComparisonOperator.GTE, 20)) {
                run(
                    Effects.ModifyStats(3, 0, EffectTarget.Self) then
                        Effects.GrantKeyword(Keyword.FIRST_STRIKE, EffectTarget.Self) then
                        Effects.GrantKeyword(Keyword.DEATHTOUCH, EffectTarget.Self)
                )
            }
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "111"
        artist = "Michele Parisi"
        imageUri = "https://cards.scryfall.io/normal/front/9/f/9f88de61-8dee-46b3-91b2-d4cdb4db36bf.jpg?1783926492"
        ruling("2021-07-23", "The instruction to roll a die and the effect that occurs because of the result are all part of the same ability. Players do not get the chance to respond to the ability after knowing the result of the roll.")
    }
}
