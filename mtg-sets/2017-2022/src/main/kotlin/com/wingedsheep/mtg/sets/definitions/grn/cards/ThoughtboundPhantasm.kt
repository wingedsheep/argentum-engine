package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CanAttackDespiteDefender
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Thoughtbound Phantasm
 * {U}
 * Creature — Spirit
 * 2/2
 * Defender
 * Whenever you surveil, put a +1/+1 counter on this creature.
 * As long as this creature has three or more +1/+1 counters on it, it can attack as though it
 * didn't have defender.
 */
val ThoughtboundPhantasm = card("Thoughtbound Phantasm") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Creature — Spirit"
    oracleText = "Defender\n" +
        "Whenever you surveil, put a +1/+1 counter on this creature.\n" +
        "As long as this creature has three or more +1/+1 counters on it, it can attack as though it didn't have defender."
    power = 2
    toughness = 2

    keywords(Keyword.DEFENDER)

    triggeredAbility {
        trigger = Triggers.you.surveils()
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    staticAbility {
        ability = CanAttackDespiteDefender(
            condition = Conditions.SourceCounterCountAtLeast(CounterType.PLUS_ONE_PLUS_ONE, 3)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "55"
        artist = "Johann Bodin"
        imageUri = "https://cards.scryfall.io/normal/front/3/6/36478e73-162a-412c-bf5b-9ca0d92f51e2.jpg?1783934182"
    }
}
