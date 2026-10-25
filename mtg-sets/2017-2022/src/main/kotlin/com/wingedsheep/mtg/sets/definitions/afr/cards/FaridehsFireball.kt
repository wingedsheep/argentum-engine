package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Farideh's Fireball
 * {3}{R}{R}
 * Instant
 * Farideh's Fireball deals 5 damage to target creature or planeswalker. Roll a d20.
 * 1—9 | Farideh's Fireball deals 2 damage to each player.
 * 10—20 | Farideh's Fireball deals 2 damage to each opponent.
 */
val FaridehsFireball = card("Farideh's Fireball") {
    manaCost = "{3}{R}{R}"
    typeLine = "Instant"
    oracleText = "Farideh's Fireball deals 5 damage to target creature or planeswalker. Roll a d20.\n" +
        "1—9 | Farideh's Fireball deals 2 damage to each player.\n" +
        "10—20 | Farideh's Fireball deals 2 damage to each opponent."

    spell {
        val victim = target(Targets.CreatureOrPlaneswalker)
        effect = Effects.Pipeline {
            run(Effects.DealDamage(5, victim))
            val result = rollDiceResult(sides = 20)
            ifThen(Conditions.CompareAmounts(result.amount, ComparisonOperator.LTE, 9)) {
                run(Effects.DealDamage(2, EffectTarget.PlayerRef(Player.Each)))
            }
            ifThen(Conditions.CompareAmounts(result.amount, ComparisonOperator.GTE, 10)) {
                run(Effects.DealDamage(2, EffectTarget.PlayerRef(Player.EachOpponent)))
            }
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "142"
        artist = "Josu Hernaiz"
        imageUri = "https://cards.scryfall.io/normal/front/5/7/57a46987-f05a-4b83-af56-f18000874e65.jpg?1783926479"
    }
}
