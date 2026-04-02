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
 * Abzan Battle Priest
 * {3}{W}
 * Creature — Human Cleric
 * 3/2
 * Outlast {W} ({W}, {T}: Put a +1/+1 counter on this creature. Outlast only as a sorcery.)
 * Each creature you control with a +1/+1 counter on it has lifelink.
 */
val AbzanBattlePriest = card("Abzan Battle Priest") {
    manaCost = "{3}{W}"
    typeLine = "Creature — Human Cleric"
    power = 3
    toughness = 2
    oracleText = "Outlast {W} ({W}, {T}: Put a +1/+1 counter on this creature. Outlast only as a sorcery.)\nEach creature you control with a +1/+1 counter on it has lifelink."

    // Outlast {W}
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{W}"), Costs.Tap)
        timing = TimingRule.SorcerySpeed
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    // Each creature you control with a +1/+1 counter on it has lifelink.
    staticAbility { ability = GrantKeywordByCounter(Keyword.LIFELINK, Counters.PLUS_ONE_PLUS_ONE, controllerOnly = true) }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "1"
        artist = "Chris Rahn"
        flavorText = "\"Wherever I walk, the ancestors walk too.\""
        imageUri = "https://cards.scryfall.io/normal/front/8/f/8f5427b1-f1c2-4bb3-8736-701667ac2256.jpg?1562790236"
    }
}
