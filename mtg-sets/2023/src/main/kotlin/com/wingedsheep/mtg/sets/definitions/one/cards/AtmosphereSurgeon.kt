package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Atmosphere Surgeon
 * {1}{U}
 * Creature — Phyrexian Wizard
 * 2/1
 *
 * Whenever you cast a noncreature spell, put an oil counter on this creature.
 * Remove an oil counter from this creature: Target creature gains flying until end of turn.
 * Activate only as a sorcery.
 */
val AtmosphereSurgeon = card("Atmosphere Surgeon") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Phyrexian Wizard"
    power = 2
    toughness = 1
    oracleText = "Whenever you cast a noncreature spell, put an oil counter on this creature.\n" +
        "Remove an oil counter from this creature: Target creature gains flying until end of turn. " +
        "Activate only as a sorcery."

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Noncreature)
        effect = Effects.AddCounters(CounterType.OIL, 1, EffectTarget.Self)
    }

    activatedAbility {
        val creature = target(TargetFilter.Creature)
        cost = Costs.RemoveCounterFromSelf(CounterType.OIL, 1)
        effect = Effects.GrantKeyword(Keyword.FLYING, creature)
        timing = TimingRule.SorcerySpeed
        description = "Remove an oil counter from this creature: Target creature gains flying until end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "41"
        artist = "Campbell White"
        flavorText = "It is Jin-Gitaxias's deepest exasperation that any part of his domain still requires maintenance."
        imageUri = "https://cards.scryfall.io/normal/front/c/6/c66988ca-29d5-45ea-8d68-c8e0e4c3ffb1.jpg?1783918069"
    }
}
