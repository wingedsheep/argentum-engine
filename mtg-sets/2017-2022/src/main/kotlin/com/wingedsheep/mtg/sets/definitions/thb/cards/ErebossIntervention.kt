package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.times
import com.wingedsheep.sdk.dsl.unaryMinus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Erebos's Intervention
 * {X}{B}
 * Instant
 * Choose one —
 * • Target creature gets -X/-X until end of turn. You gain X life.
 * • Exile up to twice X target cards from graveyards.
 *
 * Heliod's Intervention's sibling: both modes read the one X paid at cast time. The first mode's
 * life gain rides on the single target, so the spell does nothing if the creature becomes an
 * illegal target (ruling). The second mode is "up to 2X" — `optional = true` with
 * `dynamicMaxCount = X * 2` (Crashing Wave's shape), each exiled through [Effects.ForEachTarget].
 */
val ErebossIntervention = card("Erebos's Intervention") {
    manaCost = "{X}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Choose one —\n" +
        "• Target creature gets -X/-X until end of turn. You gain X life.\n" +
        "• Exile up to twice X target cards from graveyards."

    spell {
        modal(chooseCount = 1) {
            mode("Target creature gets -X/-X until end of turn. You gain X life") {
                val creature = target(TargetFilter.Creature)
                val shrink = -DynamicAmounts.xValue()
                effect = Effects.ModifyStats(shrink, shrink, creature) then
                    Effects.GainLife(DynamicAmounts.xValue())
            }
            mode("Exile up to twice X target cards from graveyards") {
                targets(
                    TargetFilter.CardInGraveyard,
                    optional = true,
                    dynamicMaxCount = DynamicAmounts.xValue() * 2,
                )
                effect = Effects.ForEachTarget(Effects.Exile(EffectTarget.ContextTarget(0)))
            }
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "94"
        artist = "Mathias Kollros"
        flavorText = "\"I bring a death none can escape.\""
        imageUri = "https://cards.scryfall.io/normal/front/8/8/88e35c80-6cfe-49fc-8138-562233ccf987.jpg?1783931569"
        ruling(
            "2020-01-24",
            "If you choose the first mode and the target creature is an illegal target by the time Erebos's " +
                "Intervention tries to resolve, the spell won't resolve. You won't gain X life."
        )
    }
}
