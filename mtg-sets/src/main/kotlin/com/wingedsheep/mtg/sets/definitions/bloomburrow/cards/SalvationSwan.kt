package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GameEvent.ZoneChangeEvent
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggerSpec
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.CreateDelayedTriggerEffect
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Salvation Swan
 * {3}{W}
 * Creature — Bird Cleric
 * 3/3
 *
 * Flash
 * Flying
 * Whenever this creature or another Bird you control enters, exile up to one
 * target creature you control without flying. Return it to the battlefield under
 * its owner's control with a flying counter on it at the beginning of the next
 * end step.
 */
val SalvationSwan = card("Salvation Swan") {
    manaCost = "{3}{W}"
    typeLine = "Creature — Bird Cleric"
    power = 3
    toughness = 3
    oracleText = "Flash\nFlying\nWhenever this creature or another Bird you control enters, exile up to one target creature you control without flying. Return it to the battlefield under its owner's control with a flying counter on it at the beginning of the next end step."

    keywords(Keyword.FLASH, Keyword.FLYING)

    // Whenever this creature or another Bird you control enters
    triggeredAbility {
        trigger = TriggerSpec(
            event = ZoneChangeEvent(
                filter = GameObjectFilter.Creature.youControl().withSubtype(Subtype("Bird")),
                to = Zone.BATTLEFIELD
            ),
            binding = TriggerBinding.ANY
        )

        // Target up to one creature you control without flying
        val creature = target(
            "creature you control without flying",
            TargetCreature(
                optional = true,
                filter = TargetFilter.Creature.youControl().withoutKeyword(Keyword.FLYING)
            )
        )

        // Exile target, then return with flying counter at end step
        effect = CompositeEffect(listOf(
            MoveToZoneEffect(creature, Zone.EXILE),
            CreateDelayedTriggerEffect(
                step = Step.END,
                effect = CompositeEffect(listOf(
                    MoveToZoneEffect(creature, Zone.BATTLEFIELD),
                    Effects.AddCounters(Counters.FLYING, 1, creature)
                ))
            )
        ))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "28"
        artist = "Christina Kraus"
        imageUri = "https://cards.scryfall.io/normal/front/b/2/b2656160-d319-4530-a6e5-c418596c3f12.jpg?1721425931"
    }
}
