package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Three Tree Scribe
 * {1}{G}
 * Creature — Frog Druid
 * 2/3
 *
 * Whenever this creature or another creature you control leaves the battlefield
 * without dying, put a +1/+1 counter on target creature you control.
 */
val ThreeTreeScribe = card("Three Tree Scribe") {
    manaCost = "{1}{G}"
    typeLine = "Creature — Frog Druid"
    oracleText = "Whenever this creature or another creature you control leaves the battlefield " +
        "without dying, put a +1/+1 counter on target creature you control."
    power = 2
    toughness = 3

    triggeredAbility {
        trigger = Triggers.YourCreatureLeavesBattlefieldWithoutDying
        val t = target("creature you control", Targets.CreatureYouControl)
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "199"
        artist = "Caio Monteiro"
        flavorText = "\"The tree wants to give me a warning. I just need to help it communicate.\""
        imageUri = "https://cards.scryfall.io/normal/front/e/a/ea2ca1b3-4c1a-4be5-b321-f57db5ff0528.jpg?1721426968"
    }
}
