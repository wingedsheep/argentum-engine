package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * The Monumental Facade
 * Land — Sphere
 *
 * This land enters with two oil counters on it.
 * {T}: Add {C}.
 * {T}, Remove an oil counter from this land: Put an oil counter on target artifact or creature
 * you control. Activate only as a sorcery.
 */
val TheMonumentalFacade = card("The Monumental Facade") {
    typeLine = "Land — Sphere"
    oracleText = "This land enters with two oil counters on it.\n" +
        "{T}: Add {C}.\n" +
        "{T}, Remove an oil counter from this land: Put an oil counter on target artifact or " +
        "creature you control. Activate only as a sorcery."

    replacementEffect(
        EntersWithCounters(
            counterType = CounterType.OIL,
            count = 2,
            selfOnly = true
        )
    )

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.RemoveCounterFromSelf(CounterType.OIL, 1))
        val recipient = target(TargetFilter(GameObjectFilter.Artifact or GameObjectFilter.Creature).youControl())
        effect = Effects.AddCounters(CounterType.OIL, 1, recipient)
        timing = TimingRule.SorcerySpeed
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "255"
        artist = "Bruce Brenneise"
        imageUri = "https://cards.scryfall.io/normal/front/d/6/d6785057-0d06-4f91-b45f-c05f7c4e2b19.jpg?1783917980"
    }
}
