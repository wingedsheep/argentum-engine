package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeywordByCounter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Tuskguard Captain
 * {2}{G}
 * Creature — Human Warrior
 * 2/3
 * Outlast {G} ({G}, {T}: Put a +1/+1 counter on this creature. Outlast only as a sorcery.)
 * Each creature you control with a +1/+1 counter on it has trample.
 */
val TuskguardCaptain = card("Tuskguard Captain") {
    manaCost = "{2}{G}"
    typeLine = "Creature — Human Warrior"
    power = 2
    toughness = 3
    oracleText = "Outlast {G} ({G}, {T}: Put a +1/+1 counter on this creature. Outlast only as a sorcery.)\nEach creature you control with a +1/+1 counter on it has trample."

    // Outlast {G}
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{G}"), Costs.Tap)
        timing = TimingRule.SorcerySpeed
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    // Each creature you control with a +1/+1 counter on it has trample.
    staticAbility { ability = GrantKeywordByCounter(Keyword.TRAMPLE, Counters.PLUS_ONE_PLUS_ONE, controllerOnly = true) }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "156"
        artist = "Aaron Miller"
        flavorText = "\"One quiet word sets off the stampede.\""
        imageUri = "https://cards.scryfall.io/normal/front/b/3/b341cc65-d316-41bb-95b8-294afa019a71.jpg?1562792160"
    }
}
