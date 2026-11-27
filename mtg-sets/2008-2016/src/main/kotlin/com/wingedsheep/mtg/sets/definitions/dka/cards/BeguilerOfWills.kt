package com.wingedsheep.mtg.sets.definitions.dka.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Beguiler of Wills — Dark Ascension #28
 * {3}{U}{U} · Creature — Human Wizard · 1/1
 *
 * {T}: Gain control of target creature with power less than or equal to the number of creatures
 * you control.
 *
 * The power cap is a dynamic target filter, checked as the target is chosen and again on
 * resolution (2011-01-22 ruling); the control change itself is permanent.
 */
val BeguilerOfWills = card("Beguiler of Wills") {
    manaCost = "{3}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Wizard"
    oracleText = "{T}: Gain control of target creature with power less than or equal to the number of " +
        "creatures you control."
    power = 1
    toughness = 1

    activatedAbility {
        cost = Costs.Tap
        val creature = target(
            TargetFilter(GameObjectFilter.Creature.powerAtMostDynamic(DynamicAmounts.creaturesYouControl()))
        )
        effect = Effects.GainControl(creature)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "28"
        artist = "Eric Deschamps"
        flavorText = "\"Come, let me free you of the tyranny of thought.\""
        imageUri = "https://cards.scryfall.io/normal/front/e/2/e21ae024-d565-48a1-8004-5aa320a5d24d.jpg?1783940846"
        ruling(
            "2011-01-22",
            "In order to activate the ability, you must be able to target a creature with power less than or " +
                "equal to the number of creatures you control. The comparison is made again when the ability " +
                "resolves. If the power is greater than the number of creatures you control at that time, the " +
                "target is illegal and the ability doesn't resolve.",
        )
        ruling(
            "2011-01-22",
            "The control-changing effect lasts indefinitely, even if the power of the creature becomes greater " +
                "than the number of creatures you control after the ability resolves.",
        )
    }
}
