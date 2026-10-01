package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Urabrask's Forge — Phyrexia: All Will Be One #153
 * {2}{R} · Artifact
 *
 * At the beginning of combat on your turn, put an oil counter on this artifact, then create an
 * X/1 red Phyrexian Horror creature token with trample and haste, where X is the number of oil
 * counters on this artifact. Sacrifice that token at the beginning of the next end step.
 *
 * X is counted once, after the oil counter lands, and the token's power is fixed at creation.
 */
val UrabrasksForge = card("Urabrask's Forge") {
    manaCost = "{2}{R}"
    typeLine = "Artifact"
    oracleText = "At the beginning of combat on your turn, put an oil counter on this artifact, then create an X/1 red Phyrexian Horror creature token with trample and haste, where X is the number of oil counters on this artifact. Sacrifice that token at the beginning of the next end step."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.BEGIN_COMBAT)
        effect = Effects.AddCounters(CounterType.OIL, 1, EffectTarget.Self) then
            Effects.CreateToken(
                power = 0,
                toughness = 1,
                colors = setOf(Color.RED),
                creatureTypes = setOf("Phyrexian", "Horror"),
                keywords = setOf(Keyword.TRAMPLE, Keyword.HASTE),
                dynamicPower = DynamicAmounts.countersOnSelf(CounterType.OIL),
                sacrificeAtStep = Step.END,
                imageUri = "https://cards.scryfall.io/normal/front/d/a/da13c90f-9ed2-4262-88e2-17daa294537b.jpg?1783918169"
            )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "153"
        artist = "Lie Setiawan"
        imageUri = "https://cards.scryfall.io/normal/front/6/8/68f9f9d8-9ea0-4608-a79c-a09a87918186.jpg?1783918022"
    }
}
