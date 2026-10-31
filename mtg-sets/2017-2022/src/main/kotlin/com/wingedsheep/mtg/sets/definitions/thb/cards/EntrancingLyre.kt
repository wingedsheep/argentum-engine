package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.values.CardNumericProperty

/**
 * Entrancing Lyre
 * {3}
 * Artifact
 * You may choose not to untap this artifact during your untap step.
 * {X}, {T}: Tap target creature with power X or less. It doesn't untap during its controller's
 * untap step for as long as this artifact remains tapped.
 *
 * The Phyrexian Gremlins shape. The source-tapped duration counts from activation, so a Lyre
 * untapped (and even re-tapped) before the ability resolves still taps the creature but doesn't
 * hold it down (ruling).
 */
val EntrancingLyre = card("Entrancing Lyre") {
    manaCost = "{3}"
    typeLine = "Artifact"
    oracleText = "You may choose not to untap this artifact during your untap step.\n" +
        "{X}, {T}: Tap target creature with power X or less. It doesn't untap during its controller's " +
        "untap step for as long as this artifact remains tapped."

    flags(AbilityFlag.MAY_NOT_UNTAP)

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{X}"), Costs.Tap)
        val creature = target(
            TargetFilter(
                GameObjectFilter.Creature.compareNumericProperty(
                    CardNumericProperty.POWER,
                    ComparisonOperator.LTE,
                    DynamicAmounts.xValue(),
                )
            )
        )
        effect = Effects.Tap(creature) then
            Effects.GrantKeyword(AbilityFlag.DOESNT_UNTAP, creature, Duration.WhileSourceTapped("this artifact"))
        description = "{X}, {T}: Tap target creature with power X or less. It doesn't untap during its " +
            "controller's untap step for as long as this artifact remains tapped."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "233"
        artist = "Yeong-Hao Han"
        imageUri = "https://cards.scryfall.io/normal/front/0/6/064abee6-7394-4b75-946f-4ad9840034ac.jpg?1783931515"
    }
}
