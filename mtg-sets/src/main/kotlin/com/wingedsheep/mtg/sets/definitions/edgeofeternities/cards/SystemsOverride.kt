package com.wingedsheep.mtg.sets.definitions.edgeofeternities.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.AddCountersEffect
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.CreateDelayedTriggerEffect
import com.wingedsheep.sdk.core.Step

/**
 * Systems Override
 * {2}{R}
 * Sorcery
 * Gain control of target artifact or creature until end of turn. Untap that permanent. It gains haste until end of turn. If it's a Spacecraft, put ten charge counters on it. If you do, remove ten charge counters from it at the beginning of the next end step.
 */
val SystemsOverride = card("Systems Override") {
    manaCost = "{2}{R}"
    typeLine = "Sorcery"
    oracleText = "Gain control of target artifact or creature until end of turn. Untap that permanent. It gains haste until end of turn. If it's a Spacecraft, put ten charge counters on it. If you do, remove ten charge counters from it at the beginning of the next end step."

    spell {
        val target = target("target artifact or creature", Targets.CreatureOrArtifact)
        effect = Effects.Composite(
            listOf(
                Effects.GainControl(target, Duration.EndOfTurn),
                Effects.Untap(target),
                Effects.GrantKeyword(Keyword.HASTE, target),
                ConditionalEffect(
                    condition = Conditions.TargetMatchesFilter(GameObjectFilter.Any.withSubtype("Spacecraft")),
                    effect = Effects.Composite(
                        listOf(
                            AddCountersEffect(Counters.CHARGE, 10, target),
                            CreateDelayedTriggerEffect(
                                step = Step.END,
                                effect = Effects.RemoveCounters(Counters.CHARGE, 10, target)
                            )
                        )
                    )
                )
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "161"
        artist = "Hardy Fowler"
        imageUri = "https://cards.scryfall.io/normal/front/2/a/2a34c71b-8d3c-435b-9cf8-4902f997d10d.jpg?1752947203"
    }
}
