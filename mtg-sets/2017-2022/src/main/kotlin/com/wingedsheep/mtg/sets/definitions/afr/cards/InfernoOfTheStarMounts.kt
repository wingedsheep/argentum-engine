package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Inferno of the Star Mounts — Adventures in the Forgotten Realms #151
 * {4}{R}{R} · Legendary Creature — Dragon 6/6 · Mythic
 *
 * "When its power becomes 20 this way" is a reflexive triggered ability (CR 603.12) linked to the
 * pump: the activation snapshots Inferno's (projected) power, applies +1/+0, and only if the
 * power was not 20 before and is exactly 20 now does the damage ability go on the stack — with
 * its "any target" chosen then, not at activation. Reaching 20 any other way, or going 20 -> 21,
 * doesn't trigger (rulings 2021-07-23); lowering its power lets it trigger again.
 */
val InfernoOfTheStarMounts = card("Inferno of the Star Mounts") {
    manaCost = "{4}{R}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Dragon"
    power = 6
    toughness = 6
    oracleText = "This spell can't be countered.\n" +
        "Flying, haste\n" +
        "{R}: Inferno of the Star Mounts gets +1/+0 until end of turn. When its power becomes 20 " +
        "this way, it deals 20 damage to any target."

    cantBeCountered = true

    keywords(Keyword.FLYING, Keyword.HASTE)

    activatedAbility {
        cost = Costs.Mana("{R}")
        val power = DynamicAmounts.sourcePower()
        effect = Effects.Pipeline(
            descriptionOverride = "Inferno of the Star Mounts gets +1/+0 until end of turn. " +
                "When its power becomes 20 this way, it deals 20 damage to any target."
        ) {
            val before = storeNumber(power)
            run(Effects.ModifyStats(1, 0, EffectTarget.Self))
            run(Effects.If(
                Conditions.All(
                    Conditions.CompareAmounts(before.amount, ComparisonOperator.NEQ, 20),
                    Conditions.CompareAmounts(power, ComparisonOperator.EQ, 20)
                ),
                Effects.ReflexiveTrigger(
                    action = Effects.Nothing,
                    optional = false,
                    descriptionOverride = "Inferno of the Star Mounts deals 20 damage to any target"
                ) {
                    val anyTarget = target(Targets.Any)
                    effect = Effects.DealDamage(20, anyTarget)
                }
            ))
        }
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "151"
        artist = "Jesper Ejsing"
        flavorText = "The ancient dragon Imvaernarhro has earned his fiery nickname."
        imageUri = "https://cards.scryfall.io/normal/front/c/7/c788c6a6-20d9-4a93-a898-330b085226c4.jpg?1783926476"
        ruling("2021-07-23", "You don't choose a target when you activate the activated ability. You choose a target for the linked triggered ability only if it triggers because the power of Inferno of the Star Mounts became 20.")
        ruling("2021-07-23", "That triggered ability triggers only if the activated ability causes the power of Inferno of the Star Mounts to become exactly 20. If its power becomes 20 in any other way, the ability won't trigger. If its power becomes 21 or greater this way, the ability won't trigger.")
        ruling("2021-07-23", "However, if you have a way to decrease its power, you can cause the triggered ability to trigger multiple times in a single turn.")
    }
}
