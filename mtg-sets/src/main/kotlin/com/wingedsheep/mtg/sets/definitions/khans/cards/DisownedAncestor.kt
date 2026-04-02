package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Disowned Ancestor
 * {B}
 * Creature — Spirit Warrior
 * 0/4
 * Outlast {1}{B} ({1}{B}, {T}: Put a +1/+1 counter on this creature. Outlast only as a sorcery.)
 */
val DisownedAncestor = card("Disowned Ancestor") {
    manaCost = "{B}"
    typeLine = "Creature — Spirit Warrior"
    power = 0
    toughness = 4
    oracleText = "Outlast {1}{B} ({1}{B}, {T}: Put a +1/+1 counter on this creature. Outlast only as a sorcery.)"

    // Outlast {1}{B}
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{B}"), Costs.Tap)
        timing = TimingRule.SorcerySpeed
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "70"
        artist = "Zack Stella"
        flavorText = "Long after death, the spirits of the Disowned continue to seek redemption among their Abzan kin."
        imageUri = "https://cards.scryfall.io/normal/front/2/d/2d8bd496-632d-4d6d-a529-f35cf3222de7.jpg?1562784359"
    }
}
