package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Steppe Glider
 * {4}{W}
 * Creature — Elemental
 * 2/4
 * Flying, vigilance
 * {1}{W}: Target creature with a +1/+1 counter on it gains flying and vigilance until end of turn.
 */
val SteppeGlider = card("Steppe Glider") {
    manaCost = "{4}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Elemental"
    power = 2
    toughness = 4
    oracleText = "Flying, vigilance\n" +
        "{1}{W}: Target creature with a +1/+1 counter on it gains flying and vigilance until end of turn."

    keywords(Keyword.FLYING, Keyword.VIGILANCE)

    activatedAbility {
        cost = Costs.Mana("{1}{W}")
        val creature = target(TargetFilter(GameObjectFilter.Creature.withCounter(CounterType.PLUS_ONE_PLUS_ONE)))
        effect = Effects.GrantKeyword(Keyword.FLYING, creature) then
            Effects.GrantKeyword(Keyword.VIGILANCE, creature)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "36"
        artist = "John Severin Brassell"
        flavorText = "It glides over the landscape. It also happens to be one."
        imageUri = "https://cards.scryfall.io/normal/front/5/3/539dbdfe-3e32-409d-8e1e-aeb0c56656a7.jpg"
    }
}
