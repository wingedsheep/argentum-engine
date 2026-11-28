package com.wingedsheep.mtg.sets.definitions.dka.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Jar of Eyeballs
 * {3}
 * Artifact
 * Whenever a creature you control dies, put two eyeball counters on this artifact.
 * {3}, {T}, Remove all eyeball counters from this artifact: Look at the top X cards of your
 * library, where X is the number of eyeball counters removed this way. Put one of them into your
 * hand and the rest on the bottom of your library in any order.
 *
 * The counters are gone by resolution, so X reads [DynamicAmounts.countersRemovedAsCost].
 */
val JarOfEyeballs = card("Jar of Eyeballs") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "Whenever a creature you control dies, put two eyeball counters on this artifact.\n" +
        "{3}, {T}, Remove all eyeball counters from this artifact: Look at the top X cards of your " +
        "library, where X is the number of eyeball counters removed this way. Put one of them into " +
        "your hand and the rest on the bottom of your library in any order."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl()).dies()
        effect = Effects.AddCounters(CounterType.EYEBALL, 2, EffectTarget.Self)
        description = "Whenever a creature you control dies, put two eyeball counters on this artifact."
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{3}"),
            Costs.Tap,
            Costs.RemoveAllCountersFromSelf(CounterType.EYEBALL),
        )
        effect = Patterns.Library.lookAtTopAndKeep(
            count = DynamicAmounts.countersRemovedAsCost(),
            keepCount = DynamicAmounts.fixed(1),
            keepDestination = CardDestination.ToZone(Zone.HAND),
            restDestination = CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Bottom),
            restOrder = CardOrder.ControllerChooses,
        )
        description = "{3}, {T}, Remove all eyeball counters from this artifact: Look at the top X cards " +
            "of your library, where X is the number of eyeball counters removed this way. Put one of " +
            "them into your hand and the rest on the bottom of your library in any order."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "152"
        artist = "Jaime Jones"
        imageUri = "https://cards.scryfall.io/normal/front/7/2/72825270-d5c1-4ab1-903a-e2868ade17f2.jpg?1783940791"
    }
}
