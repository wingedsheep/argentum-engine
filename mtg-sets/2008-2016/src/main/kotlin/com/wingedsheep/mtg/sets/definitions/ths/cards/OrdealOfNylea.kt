package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Ordeal of Nylea
 * {1}{G}
 * Enchantment — Aura
 *
 * Enchant creature
 * Whenever enchanted creature attacks, put a +1/+1 counter on it. Then if it has three or more
 * +1/+1 counters on it, sacrifice this Aura.
 * When you sacrifice this Aura, search your library for up to two basic land cards, put them
 * onto the battlefield tapped, then shuffle.
 */
val OrdealOfNylea = card("Ordeal of Nylea") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Whenever enchanted creature attacks, put a +1/+1 counter on it. Then if it has three or more " +
        "+1/+1 counters on it, sacrifice this Aura.\n" +
        "When you sacrifice this Aura, search your library for up to two basic land cards, put them " +
        "onto the battlefield tapped, then shuffle."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    // Whenever enchanted creature attacks, put a +1/+1 counter on it.
    // Then if it has three or more +1/+1 counters on it, sacrifice this Aura.
    triggeredAbility {
        trigger = Triggers.attached.attacks()
        // EffectTarget.TriggeringEntity resolves to the enchanted (attacking) creature here —
        // AttachmentTriggerDetector sets triggeringEntityId to the attached entity.
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.EnchantedCreature) then
            Effects.If(
                condition = Conditions.CompareAmounts(
                    left = DynamicAmounts.countersOnTriggering(CounterType.PLUS_ONE_PLUS_ONE),
                    operator = ComparisonOperator.GTE,
                    right = 3
                ),
                then = Effects.SacrificeTarget(EffectTarget.Self)
            )
    }

    // When you sacrifice this Aura, search your library for up to two basic land cards,
    // put them onto the battlefield tapped, then shuffle.
    triggeredAbility {
        trigger = Triggers.self.isSacrificed()
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.BasicLand,
            count = 2,
            destination = SearchDestination.BATTLEFIELD,
            entersTapped = true,
            shuffleAfter = true
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "170"
        artist = "David Palumbo"
        imageUri = "https://cards.scryfall.io/normal/front/e/5/e5c48950-c246-47ad-94e1-bf42a62c2fe7.jpg?1783939741"
    }
}
