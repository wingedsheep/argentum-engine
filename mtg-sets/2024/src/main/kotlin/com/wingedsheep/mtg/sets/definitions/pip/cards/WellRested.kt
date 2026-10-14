package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedTriggeredAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantTriggeredAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Well Rested
 * {1}{G}
 * Enchantment — Aura
 *
 * Enchant creature
 * Enchanted creature has "Whenever this creature becomes untapped, put two +1/+1 counters on it,
 * then you gain 2 life and draw a card. This ability triggers only once each turn."
 *
 * The ability lives on the creature ([GrantTriggeredAbility] over the enchanted creature), so "you"
 * is the creature's controller and the `oncePerTurn` cap is tracked per creature per turn.
 */
val WellRested = card("Well Rested") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature has \"Whenever this creature becomes untapped, put two +1/+1 counters on it, " +
        "then you gain 2 life and draw a card. This ability triggers only once each turn.\""

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = GrantTriggeredAbility(
            ability = grantedTriggeredAbility {
                trigger = Triggers.self.becomesUntapped()
                effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 2, EffectTarget.Self) then
                    Effects.GainLife(2) then
                    Effects.DrawCards(1)
                oncePerTurn = true
                description = "Whenever this creature becomes untapped, put two +1/+1 counters on it, " +
                    "then you gain 2 life and draw a card. This ability triggers only once each turn."
            },
            filter = Filters.EnchantedCreature,
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "88"
        artist = "Brian Valeza"
        flavorText = "When you're a deep sleeper, any bed will do."
        imageUri = "https://cards.scryfall.io/normal/front/e/d/ed640359-01e2-4a96-a4ab-aca053a2498d.jpg?1783912393"
    }
}
