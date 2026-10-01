package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Tamiyo's Immobilizer — Phyrexia: All Will Be One #69
 * {3}{U}
 * Artifact
 *
 * This artifact enters with four oil counters on it.
 * {T}, Remove an oil counter from this artifact: Tap target artifact or creature.
 */
val TamiyosImmobilizer = card("Tamiyo's Immobilizer") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Artifact"
    oracleText = "This artifact enters with four oil counters on it.\n" +
        "{T}, Remove an oil counter from this artifact: Tap target artifact or creature."

    replacementEffect(EntersWithCounters(counterType = CounterType.OIL, count = 4, selfOnly = true))

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.RemoveCounterFromSelf(CounterType.OIL, 1))
        val t = target(TargetFilter(GameObjectFilter.Artifact or GameObjectFilter.Creature))
        effect = Effects.Tap(target = t)
        description = "{T}, Remove an oil counter from this artifact: Tap target artifact or creature."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "69"
        artist = "Daren Bader"
        flavorText = "Visitors to the Surgical Bay often become permanent residents."
        imageUri = "https://cards.scryfall.io/normal/front/d/a/daa20112-2955-439d-8802-3a228ba0832e.jpg?1783918057"
    }
}
