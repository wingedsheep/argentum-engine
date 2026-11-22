package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Forced Adaptation
 * {G}
 * Enchantment — Aura
 * Enchant creature
 * At the beginning of your upkeep, put a +1/+1 counter on enchanted creature.
 */
val ForcedAdaptation = card("Forced Adaptation") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\nAt the beginning of your upkeep, put a +1/+1 counter on enchanted creature."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.EnchantedCreature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "120"
        artist = "Trevor Claxton"
        flavorText = "\"We're planting the seeds of change. Sometimes, we plant them in people.\"\n—Vorel of the Hull Clade"
        imageUri = "https://cards.scryfall.io/normal/front/a/6/a6527d61-e9d3-44d0-833e-19c072309270.jpg"
    }
}
