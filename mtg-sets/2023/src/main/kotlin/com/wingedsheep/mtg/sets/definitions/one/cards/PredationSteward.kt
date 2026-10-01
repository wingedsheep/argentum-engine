package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Predation Steward — Phyrexia: All Will Be One #180
 * {1}{G}
 * Creature — Phyrexian Elf Warrior
 * 2/2
 *
 * This creature enters with two oil counters on it.
 * {2}{G}, {T}, Remove an oil counter from this creature: Target creature gets +2/+2 until end of turn.
 * Activate only as a sorcery.
 */
val PredationSteward = card("Predation Steward") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Phyrexian Elf Warrior"
    power = 2
    toughness = 2
    oracleText = "This creature enters with two oil counters on it.\n" +
        "{2}{G}, {T}, Remove an oil counter from this creature: Target creature gets +2/+2 until end of turn. " +
        "Activate only as a sorcery."

    replacementEffect(EntersWithCounters(counterType = CounterType.OIL, count = 2, selfOnly = true))

    activatedAbility {
        val creature = target(TargetFilter.Creature)
        cost = Costs.Composite(
            Costs.Mana("{2}{G}"),
            Costs.Tap,
            Costs.RemoveCounterFromSelf(CounterType.OIL, 1),
        )
        effect = Effects.ModifyStats(2, 2, creature)
        timing = TimingRule.SorcerySpeed
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "180"
        artist = "Aaron J. Riley"
        flavorText = "\"Best we move on. That gurgling means it's feeding time.\"\n—Melira"
        imageUri = "https://cards.scryfall.io/normal/front/4/1/419fab07-f921-4eff-91e2-1974ae121077.jpg?1783918011"
    }
}
