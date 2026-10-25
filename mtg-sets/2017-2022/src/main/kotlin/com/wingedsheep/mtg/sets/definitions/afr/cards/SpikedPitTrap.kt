package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Spiked Pit Trap
 * {1}
 * Artifact
 * Flash
 * {5}, {T}, Sacrifice this artifact: Choose target creature, then roll a d20.
 * 1—9 | This artifact deals 5 damage to that creature.
 * 10—20 | This artifact deals 5 damage to that creature. Create a Treasure token.
 *
 * The target is chosen on activation; the d20 is rolled on resolution. The damage source is the
 * sacrificed artifact, resolved from last-known information like any sacrifice-to-deal-damage ability.
 */
val SpikedPitTrap = card("Spiked Pit Trap") {
    manaCost = "{1}"
    typeLine = "Artifact"
    oracleText = "Flash\n" +
        "{5}, {T}, Sacrifice this artifact: Choose target creature, then roll a d20.\n" +
        "1—9 | This artifact deals 5 damage to that creature.\n" +
        "10—20 | This artifact deals 5 damage to that creature. Create a Treasure token."

    keywords(Keyword.FLASH)

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{5}"), Costs.Tap, Costs.SacrificeSelf)
        val victim = target(TargetFilter.Creature)
        description = "Roll a d20. 1—9: this artifact deals 5 damage to target creature. 10—20: this artifact " +
            "deals 5 damage to that creature and you create a Treasure token."
        effect = Effects.Pipeline {
            val result = rollDiceResult(sides = 20)
            ifThen(Conditions.CompareAmounts(result.amount, ComparisonOperator.LTE, 9)) {
                run(Effects.DealDamage(5, victim))
            }
            ifThen(Conditions.CompareAmounts(result.amount, ComparisonOperator.GTE, 10)) {
                run(Effects.DealDamage(5, victim))
                run(Effects.CreateTreasure(1))
            }
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "251"
        artist = "Deruchenko Alexander"
        imageUri = "https://cards.scryfall.io/normal/front/5/9/59fbf45a-23b4-4d06-abd9-4cc3895cb29d.jpg?1783926437"
    }
}
