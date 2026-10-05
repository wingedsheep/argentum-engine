package com.wingedsheep.mtg.sets.definitions.m15.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Cruel Sadist
 * {B}
 * Creature — Human Assassin
 * 1/1
 * {B}, {T}, Pay 1 life: Put a +1/+1 counter on this creature.
 * {2}{B}, {T}, Remove X +1/+1 counters from this creature: It deals X damage to target creature.
 *
 * The second cost removes counters from the source itself (`self = true`), which caps X by the
 * +1/+1 counters it has — any of them, not only those its first ability put there (2014-07-18 ruling).
 */
val CruelSadist = card("Cruel Sadist") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Assassin"
    oracleText = "{B}, {T}, Pay 1 life: Put a +1/+1 counter on this creature.\n" +
        "{2}{B}, {T}, Remove X +1/+1 counters from this creature: It deals X damage to target creature."
    power = 1
    toughness = 1

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{B}"), Costs.Tap, Costs.PayLife(1))
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{2}{B}"),
            Costs.Tap,
            Costs.RemoveXCounters(counterType = CounterType.PLUS_ONE_PLUS_ONE, self = true),
        )
        val creature = target(TargetFilter.Creature)
        effect = Effects.DealDamage(DynamicAmounts.xValue(), creature)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "93"
        artist = "Min Yum"
        flavorText = "Face of innocence. Hand of death.\nDesigned by Edmund McMillen"
        imageUri = "https://cards.scryfall.io/normal/front/a/3/a31c94f3-c45b-4ced-b523-15e80ae1a82d.jpg?1783939185"
        ruling(
            "2014-07-18",
            "You can remove any +1/+1 counters from Cruel Sadist to activate its second ability, not just those put on Cruel Sadist by its first ability.",
        )
    }
}
