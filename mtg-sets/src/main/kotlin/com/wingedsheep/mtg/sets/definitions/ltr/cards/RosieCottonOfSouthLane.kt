package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggerSpec
import com.wingedsheep.sdk.scripting.effects.AddCountersEffect
import com.wingedsheep.sdk.scripting.events.ControllerFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Rosie Cotton of South Lane
 * {2}{W}
 * Legendary Creature — Halfling Peasant
 * 1/1
 *
 * When Rosie Cotton enters, create a Food token. (It's an artifact with "{2}, {T},
 * Sacrifice this token: You gain 3 life.")
 * Whenever you create a token, put a +1/+1 counter on target creature you control other
 * than Rosie Cotton.
 */
val RosieCottonOfSouthLane = card("Rosie Cotton of South Lane") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Halfling Peasant"
    power = 1
    toughness = 1
    oracleText = "When Rosie Cotton enters, create a Food token. (It's an artifact with \"{2}, {T}, " +
        "Sacrifice this token: You gain 3 life.\")\n" +
        "Whenever you create a token, put a +1/+1 counter on target creature you control other " +
        "than Rosie Cotton."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.CreateFood()
    }

    triggeredAbility {
        trigger = TriggerSpec(
            event = EventPattern.TokenCreationEvent(controller = ControllerFilter.You),
            binding = TriggerBinding.ANY
        )
        target("creature you control other than Rosie Cotton", TargetCreature(filter = TargetFilter.OtherCreatureYouControl))
        effect = AddCountersEffect(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.ContextTarget(0))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "27"
        artist = "Claudiu-Antoniu Magherusan"
        imageUri = "https://cards.scryfall.io/normal/front/7/5/75338f49-1f02-4333-87e4-5779ef14e688.jpg?1686967894"
    }
}
