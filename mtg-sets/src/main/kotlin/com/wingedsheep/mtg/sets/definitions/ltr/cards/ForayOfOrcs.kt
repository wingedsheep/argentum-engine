package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ReflexiveTriggerEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty
import com.wingedsheep.sdk.scripting.values.EntityReference

/**
 * Foray of Orcs
 * {3}{R}
 * Sorcery
 *
 * Amass Orcs 2. When you do, Foray of Orcs deals X damage to target creature an opponent
 * controls, where X is the amassed Army's power.
 *
 * Modeled as a [ReflexiveTriggerEffect] — Scryfall ruling (2023-06-16): "You don't choose a
 * target for Foray of Orcs at the time you cast it. Rather, a second 'reflexive' ability
 * triggers when you amass Orcs this way. You choose a target for that ability as it goes on
 * the stack. Each player may respond to this triggered ability as normal." That window is
 * material — opponents can react after amass resolves, and the army survives even if the
 * reflexive damage fizzles.
 */
val ForayOfOrcs = card("Foray of Orcs") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Amass Orcs 2. When you do, Foray of Orcs deals X damage to target creature an opponent controls, where X is the amassed Army's power. (To amass Orcs 2, put two +1/+1 counters on an Army you control. It's also an Orc. If you don't control an Army, create a 0/0 black Orc Army creature token first.)"

    spell {
        effect = ReflexiveTriggerEffect(
            action = Effects.Amass(2, "Orc"),
            optional = false,
            reflexiveEffect = Effects.DealDamage(
                amount = DynamicAmount.EntityProperty(
                    EntityReference.AmassedArmy,
                    EntityNumericProperty.Power
                ),
                target = EffectTarget.ContextTarget(0)
            ),
            reflexiveTargetRequirements = listOf(Targets.CreatureOpponentControls)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "128"
        artist = "Yuriy Chemezov"
        imageUri = "https://cards.scryfall.io/normal/front/5/f/5fea0c66-c776-4dc7-a235-f3822521cacd.jpg?1686968948"
        ruling("2023-06-16", "You don't choose a target for Foray of Orcs at the time you cast it. Rather, a second \"reflexive\" ability triggers when you amass Orcs this way. You choose a target for that ability as it goes on the stack. Each player may respond to this triggered ability as normal.")
        ruling("2023-06-16", "Some cards refer to the \"amassed Army.\" That means the Army creature you chose to receive counters, even if no counters were placed on it for some reason.")
    }
}
