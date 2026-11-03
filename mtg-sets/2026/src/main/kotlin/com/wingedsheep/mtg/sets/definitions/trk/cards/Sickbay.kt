package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.plus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Sickbay
 * {5}
 * Artifact
 * Whenever a creature you control enters, you gain 1 life.
 * At the beginning of combat on your turn, put X +1/+1 counters on target creature you control,
 * where X is one plus the amount of life you gained this turn.
 *
 * X is read on resolution: `lifeGainedThisTurn() + 1` (the LIFE_GAINED turn tracker plus a fixed 1),
 * so life gained in response to the combat trigger still counts, and a turn with no life gained
 * still puts one counter.
 */
val Sickbay = card("Sickbay") {
    manaCost = "{5}"
    typeLine = "Artifact"
    oracleText = "Whenever a creature you control enters, you gain 1 life.\n" +
        "At the beginning of combat on your turn, put X +1/+1 counters on target creature you control, " +
        "where X is one plus the amount of life you gained this turn."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl()).enters()
        effect = Effects.GainLife(1)
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.BEGIN_COMBAT)
        val creature = target(TargetFilter.Creature.youControl())
        effect = Effects.AddDynamicCounters(
            CounterType.PLUS_ONE_PLUS_ONE,
            DynamicAmounts.lifeGainedThisTurn() + 1,
            creature,
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "268"
        artist = "Joel Chaim Holtzman"
        flavorText = "Starfleet ensigns constantly push the limits, discovering new and innovative injuries."
        imageUri = "https://cards.scryfall.io/normal/front/0/5/05735357-3cca-42be-a3ae-5b4fc1fd5f47.jpg?1793359572"
    }
}
