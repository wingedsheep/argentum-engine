package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Atarka Beastbreaker
 * {1}{G}
 * Creature — Human Warrior
 * 2/2
 *
 * Formidable — {4}{G}: This creature gets +4/+4 until end of turn. Activate only if creatures you
 * control have total power 8 or greater.
 *
 * The formidable restriction is the same total-power check as Crater Elemental, checked only on
 * activation.
 */
val AtarkaBeastbreaker = card("Atarka Beastbreaker") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Warrior"
    oracleText = "Formidable — {4}{G}: This creature gets +4/+4 until end of turn. " +
        "Activate only if creatures you control have total power 8 or greater."
    power = 2
    toughness = 2

    activatedAbility {
        cost = Costs.Mana("{4}{G}")
        effect = Effects.ModifyStats(4, 4, EffectTarget.Self)
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
        rarity = Rarity.COMMON
        collectorNumber = "174"
        artist = "Johannes Voss"
        flavorText = "He scorns the heavy winter garb of lesser warriors, trusting his anger to keep him warm."
        imageUri = "https://cards.scryfall.io/normal/front/c/8/c8ef9a03-3044-4e90-9409-c330972fcafb.jpg?1783938582"
        ruling("2015-02-25", "If you control a creature with power less than 0, use its actual power when calculating the total power of creatures you control. For example, if you control three creatures with powers 4, 5, and -2, the total power of creatures you control is 7.")
        ruling("2015-02-25", "Some formidable abilities are activated abilities that require creatures you control to have total power 8 or greater. Once you activate these abilities, it doesn't matter what happens to the total power of creatures you control.")
    }
}
