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
 * Longshot Squad
 * {3}{G}
 * Creature — Dog Archer
 * 3/3
 * Outlast {1}{G} ({1}{G}, {T}: Put a +1/+1 counter on this creature. Outlast only as a sorcery.)
 * Each creature you control with a +1/+1 counter on it has reach.
 */
val LongshotSquad = card("Longshot Squad") {
    manaCost = "{3}{G}"
    typeLine = "Creature — Dog Archer"
    power = 3
    toughness = 3
    oracleText = "Outlast {1}{G} ({1}{G}, {T}: Put a +1/+1 counter on this creature. Outlast only as a sorcery.)\nEach creature you control with a +1/+1 counter on it has reach."

    // Outlast {1}{G}
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{G}"), Costs.Tap)
        timing = TimingRule.SorcerySpeed
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    // Each creature you control with a +1/+1 counter on it has reach.
    staticAbility { ability = GrantKeywordByCounter(Keyword.REACH, Counters.PLUS_ONE_PLUS_ONE, controllerOnly = true) }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "140"
        artist = "Wesley Burt"
        imageUri = "https://cards.scryfall.io/normal/front/d/d/dd9edd62-dc18-4887-a020-4464e31b79c2.jpg?1562794643"
    }
}
