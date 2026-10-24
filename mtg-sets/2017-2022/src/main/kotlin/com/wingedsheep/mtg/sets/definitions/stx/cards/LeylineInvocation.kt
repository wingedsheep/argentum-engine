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
 * Leyline Invocation — Strixhaven: School of Mages #136 (canonical printing)
 * {5}{G} · Sorcery
 *
 * Create a 0/0 green and blue Fractal creature token. Put X +1/+1 counters on it, where X is the
 * number of lands you control.
 *
 * The Body of Research shape with X = lands you control, counted at resolution (per ruling).
 */
val LeylineInvocation = card("Leyline Invocation") {
    manaCost = "{5}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Create a 0/0 green and blue Fractal creature token. Put X +1/+1 counters on it, " +
        "where X is the number of lands you control."

    spell {
        effect = Effects.CreateToken(
            power = 0,
            toughness = 0,
            colors = setOf(Color.GREEN, Color.BLUE),
            creatureTypes = setOf(Subtype.FRACTAL.value)
        ) then
            Effects.AddDynamicCounters(
                CounterType.PLUS_ONE_PLUS_ONE,
                DynamicAmounts.landsYouControl(),
                EffectTarget.PipelineTarget(CREATED_TOKENS, 0)
            )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "136"
        artist = "Liiga Smilshkalne"
        flavorText = "\"Do you still believe my work is nothing but dry, boring equations?\"\n—Cairu, Quandrix mage-student"
        imageUri = "https://cards.scryfall.io/normal/front/4/e/4edfbed0-0c0d-4aa5-bfe0-5268aff0f958.jpg?1783927341"
        ruling("2021-04-16", "The number of lands you control is counted as Leyline Invocation resolves.")
    }
}
