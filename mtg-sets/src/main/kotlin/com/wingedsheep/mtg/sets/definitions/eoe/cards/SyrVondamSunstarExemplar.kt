package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern.ZoneChangeEvent
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggerSpec
import com.wingedsheep.sdk.scripting.conditions.Compare
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetPermanent
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty
import com.wingedsheep.sdk.scripting.values.EntityReference

/**
 * Syr Vondam, Sunstar Exemplar
 * {W}{B}
 * Legendary Creature — Human Knight
 * Vigilance, menace
 * Whenever another creature you control dies or is put into exile, put a +1/+1 counter on Syr Vondam and you gain 1 life.
 * When Syr Vondam dies or is put into exile while its power is 4 or greater, destroy up to one target nonland permanent.
 * 2/2
 */
val SyrVondamSunstarExemplar = card("Syr Vondam, Sunstar Exemplar") {
    manaCost = "{W}{B}"
    colorIdentity = "WB"
    typeLine = "Legendary Creature — Human Knight"
    power = 2
    toughness = 2
    oracleText = "Vigilance, menace\nWhenever another creature you control dies or is put into exile, put a +1/+1 counter on Syr Vondam and you gain 1 life.\nWhen Syr Vondam dies or is put into exile while its power is 4 or greater, destroy up to one target nonland permanent."

    keywords(Keyword.VIGILANCE, Keyword.MENACE)

    // Shared effect for ability 1: +1/+1 counter on self + gain 1 life
    val counterAndLife = CompositeEffect(
        listOf(
            Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self),
            Effects.GainLife(1)
        )
    )

    // Whenever another creature you control dies, put a +1/+1 counter on Syr Vondam and you gain 1 life.
    triggeredAbility {
        trigger = TriggerSpec(
            event = ZoneChangeEvent(
                filter = GameObjectFilter.Creature.youControl(),
                from = Zone.BATTLEFIELD,
                to = Zone.GRAVEYARD
            ),
            binding = TriggerBinding.OTHER
        )
        effect = counterAndLife
        description = "Whenever another creature you control dies, put a +1/+1 counter on Syr Vondam and you gain 1 life."
    }

    // Whenever another creature you control is put into exile, put a +1/+1 counter on Syr Vondam and you gain 1 life.
    triggeredAbility {
        trigger = TriggerSpec(
            event = ZoneChangeEvent(
                filter = GameObjectFilter.Creature.youControl(),
                from = Zone.BATTLEFIELD,
                to = Zone.EXILE
            ),
            binding = TriggerBinding.OTHER
        )
        effect = counterAndLife
        description = "Whenever another creature you control is put into exile, put a +1/+1 counter on Syr Vondam and you gain 1 life."
    }

    // Power-is-4-or-greater condition for ability 2
    val powerAtLeast4 = Compare(
        left = DynamicAmount.EntityProperty(
            entity = EntityReference.Source,
            numericProperty = EntityNumericProperty.Power
        ),
        operator = ComparisonOperator.GTE,
        right = DynamicAmount.Fixed(4)
    )

    // When Syr Vondam dies while its power is 4 or greater, destroy up to one target nonland permanent.
    triggeredAbility {
        trigger = Triggers.Dies
        triggerCondition = powerAtLeast4
        val permanent = target(
            "up to one target nonland permanent",
            TargetPermanent(optional = true, filter = TargetFilter.NonlandPermanent)
        )
        effect = Effects.Destroy(permanent)
        description = "When Syr Vondam dies while its power is 4 or greater, destroy up to one target nonland permanent."
    }

    // When Syr Vondam is put into exile while its power is 4 or greater, destroy up to one target nonland permanent.
    triggeredAbility {
        trigger = TriggerSpec(
            event = ZoneChangeEvent(from = Zone.BATTLEFIELD, to = Zone.EXILE),
            binding = TriggerBinding.SELF
        )
        triggerCondition = powerAtLeast4
        val permanent = target(
            "up to one target nonland permanent",
            TargetPermanent(optional = true, filter = TargetFilter.NonlandPermanent)
        )
        effect = Effects.Destroy(permanent)
        description = "When Syr Vondam is put into exile while its power is 4 or greater, destroy up to one target nonland permanent."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "231"
        artist = "Ryan Pancoast"
        imageUri = "https://cards.scryfall.io/normal/front/4/9/49554198-549b-4066-86ce-77a03fda0a2f.jpg?1752947501"
        ruling("2025-07-25", "Syr Vondam, Sunstar Exemplar's first triggered ability refers to creatures you control being put into exile from the battlefield. It won't trigger if a creature card is put into exile from another zone.")
        ruling("2025-07-25", "If Syr Vondam, Sunstar Exemplar leaves the battlefield at the same time as one or more other creatures you control die or are put into exile, its first triggered ability will trigger for each of those other creatures.")
    }
}
