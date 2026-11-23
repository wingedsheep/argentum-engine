package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.evolve
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Simic Fluxmage — Gatecrash #49
 * {2}{U} · Creature — Merfolk Wizard · 1/2
 *
 * Evolve
 * {1}{U}, {T}: Move a +1/+1 counter from this creature onto target creature.
 */
val SimicFluxmage = card("Simic Fluxmage") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Merfolk Wizard"
    power = 1
    toughness = 2
    oracleText = "Evolve (Whenever a creature you control enters, if that creature has greater power or " +
        "toughness than this creature, put a +1/+1 counter on this creature.)\n" +
        "{1}{U}, {T}: Move a +1/+1 counter from this creature onto target creature."

    evolve()

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{U}"), Costs.Tap)
        val onto = target(TargetFilter.Creature)
        effect = Effects.MoveCounters(
            counterType = CounterType.PLUS_ONE_PLUS_ONE,
            amount = DynamicAmounts.fixed(1),
            source = EffectTarget.Self,
            destination = onto
        )
        description = "{1}{U}, {T}: Move a +1/+1 counter from this creature onto target creature."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "49"
        artist = "Karl Kopinski"
        imageUri = "https://cards.scryfall.io/normal/front/5/0/50633b59-6051-4f47-9e27-538fda03b5dd.jpg?1783940135"
        ruling(
            "2013-01-24",
            "If the creature is an illegal target when Simic Fluxmage's ability tries to resolve, it won't resolve " +
                "and none of its effects will happen. No counters will be removed from Simic Fluxmage."
        )
        ruling(
            "2013-01-24",
            "To move a counter from one creature to another, the counter is removed from the first creature and " +
                "placed on the second. Any abilities that care about a counter being placed on the second creature " +
                "will apply."
        )
    }
}
