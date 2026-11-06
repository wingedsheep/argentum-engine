package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Aethersquall Ancient — Kaladesh #39
 * {5}{U}{U} · Creature — Leviathan · 6/6
 *
 * Flying
 * At the beginning of your upkeep, you get {E}{E}{E}.
 * Pay eight {E}: Return all other creatures to their owners' hands. Activate only as a sorcery.
 */
val AethersquallAncient = card("Aethersquall Ancient") {
    manaCost = "{5}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Leviathan"
    power = 6
    toughness = 6
    oracleText = "Flying\n" +
        "At the beginning of your upkeep, you get {E}{E}{E} (three energy counters).\n" +
        "Pay eight {E}: Return all other creatures to their owners' hands. Activate only as a sorcery."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.GetEnergy(3)
    }

    activatedAbility {
        cost = Costs.PayPlayerCounters(CounterType.ENERGY, 8)
        effect = Patterns.Group.returnAllToHand(GroupFilter(GameObjectFilter.Creature, excludeSelf = true))
        timing = TimingRule.SorcerySpeed
        description = "Pay eight {E}: Return all other creatures to their owners' hands."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "39"
        artist = "Sam Burley"
        imageUri = "https://cards.scryfall.io/normal/front/5/5/55127a25-dc64-4f26-ae50-ed7247c22ae6.jpg?1783937223"
    }
}
