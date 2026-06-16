package com.wingedsheep.mtg.sets.definitions.sos.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.increment
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.CREATED_TOKENS
import com.wingedsheep.sdk.scripting.effects.WardCost
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Fractal Tender
 * {3}{G}{U}
 * Creature — Elf Wizard
 * 3/3
 *
 * Ward {2}
 * Increment (Whenever you cast a spell, if the amount of mana you spent is greater than
 * this creature's power or toughness, put a +1/+1 counter on this creature.)
 * At the beginning of each end step, if you put a counter on this creature this turn,
 * create a 0/0 green and blue Fractal creature token and put three +1/+1 counters on it.
 */
val FractalTender = card("Fractal Tender") {
    manaCost = "{3}{G}{U}"
    colorIdentity = "UG"
    typeLine = "Creature — Elf Wizard"
    power = 3
    toughness = 3
    oracleText = "Ward {2}\n" +
        "Increment (Whenever you cast a spell, if the amount of mana you spent is greater than " +
        "this creature's power or toughness, put a +1/+1 counter on this creature.)\n" +
        "At the beginning of each end step, if you put a counter on this creature this turn, " +
        "create a 0/0 green and blue Fractal creature token and put three +1/+1 counters on it."

    keywordAbility(KeywordAbility.Ward(WardCost.Mana("{2}")))
    increment()

    triggeredAbility {
        trigger = Triggers.EachEndStep
        triggerCondition = Conditions.SourceReceivedCounterThisTurn
        // Create the 0/0 Fractal (publishing it to the CREATED_TOKENS pipeline collection), then put
        // three +1/+1 counters on that just-created token via PipelineTarget(CREATED_TOKENS, 0).
        effect = Effects.Composite(
            listOf(
                Effects.CreateToken(
                    power = 0,
                    toughness = 0,
                    colors = setOf(Color.GREEN, Color.BLUE),
                    creatureTypes = setOf("Fractal"),
                    imageUri = "https://cards.scryfall.io/normal/front/d/e/de564776-9d88-4533-8717-842eecdd0594.jpg?1775828279"
                ),
                Effects.AddCounters(
                    Counters.PLUS_ONE_PLUS_ONE,
                    3,
                    EffectTarget.PipelineTarget(CREATED_TOKENS, 0)
                )
            )
        )
        description = "At the beginning of each end step, if you put a counter on this creature " +
            "this turn, create a 0/0 green and blue Fractal creature token and put three +1/+1 " +
            "counters on it."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "190"
        artist = "Elizabeth Peiró"
        imageUri = "https://cards.scryfall.io/normal/front/e/a/ea7f5262-4ddb-410a-be72-4bac6af9b4ec.jpg?1775938318"
    }
}
