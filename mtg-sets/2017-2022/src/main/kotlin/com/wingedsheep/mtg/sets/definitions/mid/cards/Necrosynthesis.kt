package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantTriggeredAbility
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Necrosynthesis — Innistrad: Midnight Hunt #115
 * {1}{B} · Enchantment — Aura · Uncommon
 *
 * Enchant creature
 * Enchanted creature has "Whenever another creature dies, put a +1/+1 counter on this creature."
 * When enchanted creature dies, look at the top X cards of your library, where X is its power.
 * Put one of those cards into your hand and the rest on the bottom of your library in a random order.
 *
 * The quoted ability is granted to the host (default self-scoped [GrantTriggeredAbility]), so the
 * counters land on the enchanted creature and `EffectTarget.Self` inside it is that creature.
 * The Aura's own dies trigger reads the creature's last-known power ([DynamicAmounts.triggeringPower]),
 * which includes the counters it had accumulated.
 */
val Necrosynthesis = card("Necrosynthesis") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature has \"Whenever another creature dies, put a +1/+1 counter on this creature.\"\n" +
        "When enchanted creature dies, look at the top X cards of your library, where X is its power. " +
        "Put one of those cards into your hand and the rest on the bottom of your library in a random order."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = GrantTriggeredAbility(
            ability = TriggeredAbility.create(
                trigger = Triggers.another(GameObjectFilter.Creature).dies(),
                effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self),
                descriptionOverride = "Whenever another creature dies, put a +1/+1 counter on this creature."
            )
        )
    }

    triggeredAbility {
        trigger = Triggers.attached.dies()
        effect = Patterns.Library.lookAtTopAndKeep(
            count = DynamicAmounts.triggeringPower(),
            keepCount = DynamicAmounts.fixed(1),
            keepDestination = CardDestination.ToZone(Zone.HAND),
            restDestination = CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Bottom),
            restOrder = CardOrder.Random
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "115"
        artist = "Isis"
        imageUri = "https://cards.scryfall.io/normal/front/b/7/b75b89f1-e032-4d8c-b2b6-1b76e8644801.jpg?1783925612"

        ruling(
            "2021-09-24",
            "Use the enchanted creature's power as it last existed on the battlefield to determine the value of X."
        )
    }
}
