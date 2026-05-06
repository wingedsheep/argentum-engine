package com.wingedsheep.mtg.sets.definitions.foundations.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.Compare
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.EntityReference
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ordeal of Nylea
 * {1}{G}
 * Enchantment — Aura
 * Enchant creature
 * Whenever enchanted creature attacks, put a +1/+1 counter on it. Then if it has three or more +1/+1 counters on it, sacrifice this Aura.
 * When you sacrifice this Aura, search your library for up to two basic land cards, put them onto the battlefield tapped, then shuffle.
 */
val OrdealOfNylea = card("Ordeal of Nylea") {
    manaCost = "{1}{G}"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\nWhenever enchanted creature attacks, put a +1/+1 counter on it. Then if it has three or more +1/+1 counters on it, sacrifice this Aura.\nWhen you sacrifice this Aura, search your library for up to two basic land cards, put them onto the battlefield tapped, then shuffle."

    // Aura target
    auraTarget = Targets.Creature

    // Whenever enchanted creature attacks, put a +1/+1 counter on it.
    // Then if it has three or more +1/+1 counters on it, sacrifice this Aura.
    triggeredAbility {
        trigger = Triggers.EnchantedCreatureAttacks
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.EnchantedCreature) then
            ConditionalEffect(
                condition = Compare(
                    left = DynamicAmount.EntityProperty(
                        entity = EntityReference.Triggering,
                        numericProperty = EntityNumericProperty.CounterCount(CounterTypeFilter.Named(Counters.PLUS_ONE_PLUS_ONE))
                    ),
                    operator = ComparisonOperator.GTE,
                    right = DynamicAmount.Fixed(3)
                ),
                effect = Effects.SacrificeTarget(EffectTarget.Self)
            )
        description = "Whenever enchanted creature attacks, put a +1/+1 counter on it. Then if it has three or more +1/+1 counters on it, sacrifice this Aura."
    }

    // When you sacrifice this Aura, search your library for up to two basic land cards,
    // put them onto the battlefield tapped, then shuffle.
    triggeredAbility {
        trigger = Triggers.Sacrificed
        effect = EffectPatterns.searchLibrary(
            filter = GameObjectFilter.BasicLand,
            count = 2,
            destination = SearchDestination.BATTLEFIELD,
            entersTapped = true,
            shuffleAfter = true
        )
        description = "When you sacrifice this Aura, search your library for up to two basic land cards, put them onto the battlefield tapped, then shuffle."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "641"
        artist = "David Palumbo"
        imageUri = "https://cards.scryfall.io/normal/front/1/a/1a70424d-86a7-44a3-acda-4463d7ac503b.jpg?1730491029"
    }
}
