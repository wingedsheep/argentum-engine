package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Norn's Wellspring
 * {1}{W}
 * Artifact
 *
 * Whenever a creature you control dies, scry 1 and put an oil counter on this artifact.
 * {1}, {T}, Remove two oil counters from this artifact: Draw a card.
 */
val NornsWellspring = card("Norn's Wellspring") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Artifact"
    oracleText = "Whenever a creature you control dies, scry 1 and put an oil counter on this artifact.\n" +
        "{1}, {T}, Remove two oil counters from this artifact: Draw a card."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl()).dies()
        effect = Effects.Scry(1) then Effects.AddCounters(CounterType.OIL, 1, EffectTarget.Self)
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Tap, Costs.RemoveCounterFromSelf(CounterType.OIL, 2))
        effect = Effects.DrawCards(1)
        description = "{1}, {T}, Remove two oil counters from this artifact: Draw a card."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "24"
        artist = "Jonas De Ro"
        flavorText = "\"Drown, so you may breathe again.\"\n—Atraxa"
        imageUri = "https://cards.scryfall.io/normal/front/e/e/eeebc0ab-89fb-47d5-a20f-8f1fe5e3c149.jpg?1783918076"
    }
}
