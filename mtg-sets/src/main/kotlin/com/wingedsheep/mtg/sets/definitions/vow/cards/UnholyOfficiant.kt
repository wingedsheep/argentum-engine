package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.AddCountersEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Unholy Officiant
 * {W}
 * Creature — Vampire Cleric
 * 1/2
 * Vigilance
 * {4}{W}: Put a +1/+1 counter on this creature.
 */
val UnholyOfficiant = card("Unholy Officiant") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Creature — Vampire Cleric"
    oracleText = "Vigilance\n{4}{W}: Put a +1/+1 counter on this creature."
    power = 1
    toughness = 2
    keywords(Keyword.VIGILANCE)
    activatedAbility {
        cost = Costs.Mana("{4}{W}")
        effect = AddCountersEffect(counterType = Counters.PLUS_ONE_PLUS_ONE, count = 1, target = EffectTarget.Self)
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "41"
        artist = "Campbell White"
        flavorText = "\"Is this twisted ceremony simply a mockery of the human rite, or has Olivia fallen for her own charade?\"\n—Sorin Markov"
        imageUri = "https://cards.scryfall.io/normal/front/8/a/8a4c04a4-e68b-4d3d-89c8-29cdaaac36b2.jpg?1782703165"
    }
}
