package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Hall of Oracles — Strixhaven: School of Mages #267
 * Land
 *
 * {T}: Add {C}.
 * {1}, {T}: Add one mana of any color.
 * {T}: Put a +1/+1 counter on target creature. Activate only as a sorcery and only if you've cast
 * an instant or sorcery spell this turn.
 *
 * The mana abilities are Mana Prism's. The counter ability is [TimingRule.SorcerySpeed] plus
 * Potioner's Trove's cast-history gate (`Conditions.YouCastSpellsThisTurn` over instants and
 * sorceries).
 */
val HallOfOracles = card("Hall of Oracles") {
    colorIdentity = ""
    typeLine = "Land"
    oracleText = "{T}: Add {C}.\n" +
        "{1}, {T}: Add one mana of any color.\n" +
        "{T}: Put a +1/+1 counter on target creature. Activate only as a sorcery and only if you've " +
        "cast an instant or sorcery spell this turn."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Tap)
        effect = Effects.AddManaOfChoice()
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Tap
        val creature = target(TargetFilter.Creature)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creature)
        timing = TimingRule.SorcerySpeed
        restrictions = listOf(
            ActivationRestriction.OnlyIfCondition(
                Conditions.YouCastSpellsThisTurn(
                    atLeast = 1,
                    filter = GameObjectFilter.InstantOrSorcery,
                ),
            ),
        )
        description = "{T}: Put a +1/+1 counter on target creature. Activate only as a sorcery and only " +
            "if you've cast an instant or sorcery spell this turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "267"
        artist = "Piotr Dura"
        imageUri = "https://cards.scryfall.io/normal/front/7/6/7642dddd-4461-46dd-b396-9e0a89f7232e.jpg?1783927274"
    }
}
