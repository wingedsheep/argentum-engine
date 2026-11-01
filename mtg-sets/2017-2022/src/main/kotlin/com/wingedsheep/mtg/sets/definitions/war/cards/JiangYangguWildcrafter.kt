package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Jiang Yanggu, Wildcrafter
 * {2}{G}
 * Legendary Planeswalker — Yanggu
 * Loyalty: 3
 *
 * Each creature you control with a +1/+1 counter on it has "{T}: Add one mana of any color."
 * −1: Put a +1/+1 counter on target creature.
 *
 * Esika's granted-mana-ability shape over Bramblewood Paragon's "creature you control with a
 * +1/+1 counter" group filter. The grant is re-evaluated continuously, so a creature gains the
 * ability the moment it gets a counter and loses it when the last one leaves.
 */
val JiangYangguWildcrafter = card("Jiang Yanggu, Wildcrafter") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Planeswalker — Yanggu"
    startingLoyalty = 3
    oracleText = "Each creature you control with a +1/+1 counter on it has \"{T}: Add one mana of any color.\"\n" +
        "−1: Put a +1/+1 counter on target creature."

    staticAbility {
        ability = GrantActivatedAbility(
            ability = ActivatedAbility(
                id = AbilityId.next(),
                cost = Costs.Tap,
                effect = Effects.AddAnyColorMana(1),
                isManaAbility = true,
                timing = TimingRule.ManaAbility,
                descriptionOverride = "{T}: Add one mana of any color."
            ),
            filter = GroupFilter(
                GameObjectFilter.Creature.youControl().withCounter(CounterType.PLUS_ONE_PLUS_ONE),
            ),
        )
    }

    loyaltyAbility(-1) {
        val creature = target(TargetFilter.Creature)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creature)
        description = "Put a +1/+1 counter on target creature."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "164"
        artist = "Anna Steinbauer"
        imageUri = "https://cards.scryfall.io/normal/front/1/0/10e0d47d-b13c-4d48-881f-8aaa347ef209.jpg?1783933410"
    }
}
