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
 * Dragon-Scarred Bear
 * {2}{G}
 * Creature — Bear
 * 3/2
 *
 * Formidable — {1}{G}: Regenerate this creature. Activate only if creatures you control have total
 * power 8 or greater.
 *
 * Atarka Beastbreaker's formidable restriction (checked only on activation) over a self-regeneration
 * shield.
 */
val DragonScarredBear = card("Dragon-Scarred Bear") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Bear"
    oracleText = "Formidable — {1}{G}: Regenerate this creature. " +
        "Activate only if creatures you control have total power 8 or greater."
    power = 3
    toughness = 2

    activatedAbility {
        cost = Costs.Mana("{1}{G}")
        effect = Effects.Regenerate(EffectTarget.Self)
        restrictions = listOf(
            ActivationRestriction.OnlyIfCondition(
                Conditions.CompareAmounts(
                    DynamicAmounts.battlefield(Player.You, GameObjectFilter.Creature).sumPower(),
                    ComparisonOperator.GTE,
                    8
                )
            )
        )
        description = "Formidable — {1}{G}: Regenerate this creature."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "183"
        artist = "Lars Grant-West"
        flavorText = "Bears are a delicacy for the Atarka. The few that remain are the toughest of their species."
        imageUri = "https://cards.scryfall.io/normal/front/6/8/689a6390-894a-4d31-87c2-f9cec3b187a7.jpg?1783938580"
        ruling("2015-02-25", "If you control a creature with power less than 0, use its actual power when calculating the total power of creatures you control. For example, if you control three creatures with powers 4, 5, and -2, the total power of creatures you control is 7.")
        ruling("2015-02-25", "Some formidable abilities are activated abilities that require creatures you control to have total power 8 or greater. Once you activate these abilities, it doesn't matter what happens to the total power of creatures you control.")
    }
}
