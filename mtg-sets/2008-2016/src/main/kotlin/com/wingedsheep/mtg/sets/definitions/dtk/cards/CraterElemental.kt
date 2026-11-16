package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Crater Elemental
 * {2}{R}
 * Creature — Elemental
 * 0/6
 *
 * {R}, {T}, Sacrifice this creature: It deals 4 damage to target creature.
 * Formidable — {2}{R}: This creature has base power 8 until end of turn. Activate only if creatures
 * you control have total power 8 or greater.
 *
 * "Has base power 8" sets only base power (layer 7b) via [Effects.SetBasePower], so pumps and
 * counters still apply on top of it and its toughness stays 6. The formidable restriction is the
 * same total-power check as Atarka Pummeler, checked only on activation.
 */
val CraterElemental = card("Crater Elemental") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Elemental"
    oracleText = "{R}, {T}, Sacrifice this creature: It deals 4 damage to target creature.\n" +
        "Formidable — {2}{R}: This creature has base power 8 until end of turn. " +
        "Activate only if creatures you control have total power 8 or greater."
    power = 0
    toughness = 6

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{R}"), Costs.Tap, Costs.SacrificeSelf)
        val creature = target(TargetFilter.Creature)
        effect = Effects.DealDamage(4, creature)
    }

    activatedAbility {
        cost = Costs.Mana("{2}{R}")
        effect = Effects.SetBasePower(power = DynamicAmounts.fixed(8), duration = Duration.EndOfTurn)
        restrictions = listOf(
            ActivationRestriction.OnlyIfCondition(
                Conditions.CompareAmounts(
                    DynamicAmounts.battlefield(Player.You, GameObjectFilter.Creature).sumPower(),
                    ComparisonOperator.GTE,
                    8
                )
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "132"
        artist = "Svetlin Velinov"
        imageUri = "https://cards.scryfall.io/normal/front/c/4/c4ee8d1f-00e8-44e3-b125-ef98e1461263.jpg?1783938591"
        ruling("2015-02-25", "Crater Elemental's last ability overrides all previous effects that set its power to a specific value. Other effects that set its power to a specific value that start to apply after the ability resolves will override this effect.")
        ruling("2015-02-25", "Effects that modify Crater Elemental's power, such as the effects of Giant Growth or Hall of Triumph, will apply to it no matter when they started to take effect. The same is true for counters that change its power (such as +1/+1 counters) and effects that switch its power and toughness.")
        ruling("2015-02-25", "If you control a creature with power less than 0, use its actual power when calculating the total power of creatures you control. For example, if you control three creatures with powers 4, 5, and -2, the total power of creatures you control is 7.")
        ruling("2015-02-25", "Some formidable abilities are activated abilities that require creatures you control to have total power 8 or greater. Once you activate these abilities, it doesn't matter what happens to the total power of creatures you control.")
    }
}
