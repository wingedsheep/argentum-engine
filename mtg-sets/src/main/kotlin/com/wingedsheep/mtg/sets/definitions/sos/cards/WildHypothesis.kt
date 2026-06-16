package com.wingedsheep.mtg.sets.definitions.sos.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CREATED_TOKENS
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Wild Hypothesis
 * {X}{G}
 * Sorcery
 *
 * Create a 0/0 green and blue Fractal creature token. Put X +1/+1 counters on it.
 * Surveil 2.
 *
 * The Fractal is created (published to the [CREATED_TOKENS] pipeline collection), then a
 * dynamic X +1/+1 counters land on that just-created token via
 * `PipelineTarget(CREATED_TOKENS, 0)` (same shape as Fractal Tender). X comes from the
 * `{X}` in the mana cost ([DynamicAmount.XValue]). Surveil 2 is the standard library
 * pattern recipe.
 */
val WildHypothesis = card("Wild Hypothesis") {
    manaCost = "{X}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Create a 0/0 green and blue Fractal creature token. Put X +1/+1 counters on it.\n" +
        "Surveil 2. (Look at the top two cards of your library, then put any number of them into " +
        "your graveyard and the rest on top of your library in any order.)"

    spell {
        effect = Effects.CreateToken(
            power = 0,
            toughness = 0,
            colors = setOf(Color.GREEN, Color.BLUE),
            creatureTypes = setOf("Fractal"),
            imageUri = "https://cards.scryfall.io/normal/front/d/e/de564776-9d88-4533-8717-842eecdd0594.jpg?1775828279"
        )
            .then(
                Effects.AddDynamicCounters(
                    Counters.PLUS_ONE_PLUS_ONE,
                    DynamicAmount.XValue,
                    EffectTarget.PipelineTarget(CREATED_TOKENS, 0)
                )
            )
            .then(Patterns.Library.surveil(2))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "167"
        artist = "Lie Setiawan"
        imageUri = "https://cards.scryfall.io/normal/front/0/4/04fdfabc-c247-4384-a5bb-f49035f8aae0.jpg?1775938142"
    }
}
