package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CREATED_TOKENS
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Fractal Summoning — Strixhaven: School of Mages #187 (canonical printing)
 * {X}{G/U}{G/U} · Sorcery — Lesson
 *
 * Create a 0/0 green and blue Fractal creature token. Put X +1/+1 counters on it.
 *
 * The Body of Research shape: [Effects.CreateToken] then [Effects.AddDynamicCounters] on the
 * created token, with the cast's X as the count. State-based actions don't run mid-resolution,
 * so the 0/0 survives until its counters land; at X = 0 it dies to them. Lesson is only a subtype.
 */
val FractalSummoning = card("Fractal Summoning") {
    manaCost = "{X}{G/U}{G/U}"
    colorIdentity = "GU"
    typeLine = "Sorcery — Lesson"
    oracleText = "Create a 0/0 green and blue Fractal creature token. Put X +1/+1 counters on it."

    spell {
        effect = Effects.CreateToken(
            power = 0,
            toughness = 0,
            colors = setOf(Color.GREEN, Color.BLUE),
            creatureTypes = setOf(Subtype.FRACTAL.value)
        ) then
            Effects.AddDynamicCounters(
                CounterType.PLUS_ONE_PLUS_ONE,
                DynamicAmounts.xValue(),
                EffectTarget.PipelineTarget(CREATED_TOKENS, 0)
            )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "187"
        artist = "Livia Prima"
        flavorText = "\"Fractals are just pseudo-dimensional manifestations of asynchronous biosymmetries. It's really quite simple.\"\n—Kianne, Quandrix dean"
        imageUri = "https://cards.scryfall.io/normal/front/c/c/cc3f1f7e-eb19-49c1-a1ee-93b85ac8815c.jpg?1783927314"
    }
}
