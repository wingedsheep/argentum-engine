package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Solstice Zealot
 * {2}{W}
 * Creature — Rhino Cleric
 * 2/3
 *
 * When this creature enters, you get {E}{E} (two energy counters).
 * {T}, Pay {E}: Tap target creature.
 *
 * The `{T}` in the cost is the Zealot tapping itself (so summoning sickness applies); the target
 * carries no controller predicate, matching the Oracle wording.
 */
val SolsticeZealot = card("Solstice Zealot") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Rhino Cleric"
    power = 2
    toughness = 3
    oracleText = "When this creature enters, you get {E}{E} (two energy counters).\n" +
        "{T}, Pay {E}: Tap target creature."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.GetEnergy(2)
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.PayPlayerCounters(CounterType.ENERGY, 1))
        val creature = target(TargetFilter.Creature)
        effect = Effects.Tap(creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "43"
        artist = "Steve Ellis"
        flavorText = "\"Embrace the light, or succumb to the shadows.\""
        imageUri = "https://cards.scryfall.io/normal/front/f/1/f10539e3-d4bc-4f71-a00f-8e94bb97509d.jpg?1783911296"
    }
}
