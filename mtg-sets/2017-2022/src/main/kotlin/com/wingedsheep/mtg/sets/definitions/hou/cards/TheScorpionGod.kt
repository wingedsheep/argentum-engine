package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * The Scorpion God — Hour of Devastation #146 (canonical printing)
 * {3}{B}{R} · Legendary Creature — God · 6/5
 *
 * Whenever a creature with a -1/-1 counter on it dies, draw a card.
 * {1}{B}{R}: Put a -1/-1 counter on another target creature.
 * When The Scorpion God dies, return it to its owner's hand at the beginning of the next end step.
 *
 * The draw trigger reads the dying creature's last-known counters, so it sees The Scorpion God
 * itself and creatures dying alongside it (ruling). The dies trigger is The Locust God's.
 */
val TheScorpionGod = card("The Scorpion God") {
    manaCost = "{3}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Legendary Creature — God"
    power = 6
    toughness = 5
    oracleText = "Whenever a creature with a -1/-1 counter on it dies, draw a card.\n" +
        "{1}{B}{R}: Put a -1/-1 counter on another target creature.\n" +
        "When The Scorpion God dies, return it to its owner's hand at the beginning of the next end step."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.withCounter(CounterType.MINUS_ONE_MINUS_ONE)).dies()
        effect = Effects.DrawCards(1)
    }

    activatedAbility {
        cost = Costs.Mana("{1}{B}{R}")
        val creature = target(TargetFilter.OtherCreature)
        effect = Effects.AddCounters(CounterType.MINUS_ONE_MINUS_ONE, 1, creature)
    }

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.CreateDelayedTrigger(
            step = Step.END,
            effect = Effects.Move(EffectTarget.TriggeringEntity, Zone.HAND, fromZone = Zone.GRAVEYARD),
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "146"
        artist = "Lius Lasahido"
        imageUri = "https://cards.scryfall.io/normal/front/d/e/def0d0ac-6422-486e-b42b-e1813dd0a23e.jpg?1783936009"
        ruling("2017-07-14", "If a creature with a -1/-1 counter on it dies at the same time as The Scorpion God does, you'll draw a card. The same is true if The Scorpion God dies with a -1/-1 counter on it.")
        ruling("2017-07-14", "You'll draw only one card when a creature with more than one -1/-1 counter on it dies.")
    }
}
