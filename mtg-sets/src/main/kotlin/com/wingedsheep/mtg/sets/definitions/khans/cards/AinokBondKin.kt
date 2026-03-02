package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeywordByCounter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ainok Bond-Kin
 * {1}{W}
 * Creature — Dog Soldier
 * 2/1
 * Outlast {1}{W} ({1}{W}, {T}: Put a +1/+1 counter on this creature. Outlast only as a sorcery.)
 * Each creature you control with a +1/+1 counter on it has first strike.
 */
val AinokBondKin = card("Ainok Bond-Kin") {
    manaCost = "{1}{W}"
    typeLine = "Creature — Dog Soldier"
    power = 2
    toughness = 1
    oracleText = "Outlast {1}{W} ({1}{W}, {T}: Put a +1/+1 counter on this creature. Outlast only as a sorcery.)\nEach creature you control with a +1/+1 counter on it has first strike."

    // Outlast {1}{W}
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{W}"), Costs.Tap)
        timing = TimingRule.SorcerySpeed
        effect = Effects.AddCounters("+1/+1", 1, EffectTarget.Self)
    }

    // Each creature you control with a +1/+1 counter on it has first strike.
    staticAbility { ability = GrantKeywordByCounter(Keyword.FIRST_STRIKE, "+1/+1", controllerOnly = true) }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "3"
        artist = "Chris Rahn"
        flavorText = "\"Hold the line, for family and the fallen!\""
        imageUri = "https://cards.scryfall.io/normal/front/2/2/22d2a844-17fc-4628-9591-684555e98f7b.jpg?1562783624"
    }
}