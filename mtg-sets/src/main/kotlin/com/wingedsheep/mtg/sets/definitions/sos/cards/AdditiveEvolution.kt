package com.wingedsheep.mtg.sets.definitions.sos.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CREATED_TOKENS
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Additive Evolution
 * {3}{G}{G}
 * Enchantment
 *
 * When this enchantment enters, create a 0/0 green and blue Fractal creature token. Put three
 * +1/+1 counters on it.
 * At the beginning of combat on your turn, put a +1/+1 counter on target creature you control. It
 * gains vigilance until end of turn.
 *
 * The ETB composes the standard SOS Fractal recipe (Fractal Anomaly / Ambitious Augmenter): create
 * the 0/0 green-and-blue Fractal (published to the [CREATED_TOKENS] pipeline collection), then put
 * three +1/+1 counters on that just-created token via `PipelineTarget(CREATED_TOKENS, 0)`. The
 * count is fixed (three), so a plain [Effects.AddCounters].
 *
 * The combat trigger uses [Triggers.BeginCombat] (already restricted to the controller's combat,
 * "on your turn"): bind one target creature you control, add a +1/+1 counter to it, then grant it
 * vigilance until end of turn — both effects reference the same bound target.
 */
val AdditiveEvolution = card("Additive Evolution") {
    manaCost = "{3}{G}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment"
    oracleText = "When this enchantment enters, create a 0/0 green and blue Fractal creature " +
        "token. Put three +1/+1 counters on it.\n" +
        "At the beginning of combat on your turn, put a +1/+1 counter on target creature you " +
        "control. It gains vigilance until end of turn."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.CreateToken(
            power = 0,
            toughness = 0,
            colors = setOf(Color.GREEN, Color.BLUE),
            creatureTypes = setOf("Fractal"),
            imageUri = "https://cards.scryfall.io/normal/front/d/e/de564776-9d88-4533-8717-842eecdd0594.jpg?1775828279"
        ).then(
            Effects.AddCounters(
                Counters.PLUS_ONE_PLUS_ONE,
                3,
                EffectTarget.PipelineTarget(CREATED_TOKENS, 0)
            )
        )
        description = "When this enchantment enters, create a 0/0 green and blue Fractal creature " +
            "token. Put three +1/+1 counters on it."
    }

    triggeredAbility {
        trigger = Triggers.BeginCombat
        val creature = target("target creature you control", Targets.CreatureYouControl)
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, creature)
            .then(Effects.GrantKeyword(Keyword.VIGILANCE, creature))
        description = "At the beginning of combat on your turn, put a +1/+1 counter on target " +
            "creature you control. It gains vigilance until end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "139"
        artist = "Josiah \"Jo\" Cameron"
        flavorText = "\"Numbers have no limits. Why should nature?\"\n—Emil, Quandrix fourth-year"
        imageUri = "https://cards.scryfall.io/normal/front/b/4/b44ec684-d558-45eb-bcd6-8119428634c2.jpg?1775937943"
    }
}
