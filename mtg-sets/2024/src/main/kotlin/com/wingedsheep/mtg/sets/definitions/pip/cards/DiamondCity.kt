package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Diamond City — Fallout #147.
 *
 * The move ability counts creatures that *entered* under your control this turn (the per-player
 * entry log, read as each entry's types at the moment it entered) — they needn't still be on the
 * battlefield. With no shield counter left on Diamond City the ability can still be activated; it
 * simply moves nothing.
 */
val DiamondCity = card("Diamond City") {
    typeLine = "Land"
    oracleText = "This land enters with a shield counter on it. (If it would be dealt damage or destroyed, remove a shield counter from it instead.)\n{T}: Add {C}.\n{T}: Move a shield counter from this land onto target creature. Activate only if two or more creatures entered the battlefield under your control this turn."
    replacementEffect(
        EntersWithCounters(
            counterType = CounterType.SHIELD,
            count = 1,
            selfOnly = true,
        )
    )
    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }
    activatedAbility {
        cost = Costs.Tap
        val creature = target(TargetFilter.Creature)
        effect = Effects.MoveCounters(CounterType.SHIELD, DynamicAmounts.fixed(1), EffectTarget.Self, creature)
        restrictions = listOf(
            ActivationRestriction.OnlyIfCondition(
                Conditions.CompareAmounts(
                    left = DynamicAmounts.cardTypeEnteredUnderControlThisTurn(CardType.CREATURE),
                    operator = ComparisonOperator.GTE,
                    right = 2,
                )
            )
        )
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "147"
        artist = "Pascal Quidault"
        imageUri = "https://cards.scryfall.io/normal/front/3/e/3e9bd49a-e9f1-4543-b04a-777a9e5a55ec.jpg?1783912374"
        ruling("2024-03-08", "Shield counters don’t prevent players from sacrificing permanents.")
        ruling("2024-03-08", "If a permanent that would be dealt damage has more than one shield counter on it, that damage is prevented and only one shield counter is removed.")
    }
}
