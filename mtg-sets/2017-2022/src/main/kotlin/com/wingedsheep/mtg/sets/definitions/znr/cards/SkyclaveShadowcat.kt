package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Skyclave Shadowcat — Zendikar Rising #126
 * {3}{B} · Creature — Cat Horror · 3/3
 *
 * {1}{B}, Sacrifice another creature: Put a +1/+1 counter on this creature.
 * Whenever a creature you control with a +1/+1 counter on it dies, draw a card.
 *
 * The dies trigger is the ANY binding ("a creature you control"), so the Shadowcat itself counts once
 * it carries a counter. The counter check reads the creature's last-known counters as it left.
 */
val SkyclaveShadowcat = card("Skyclave Shadowcat") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Cat Horror"
    oracleText = "{1}{B}, Sacrifice another creature: Put a +1/+1 counter on this creature.\n" +
        "Whenever a creature you control with a +1/+1 counter on it dies, draw a card."
    power = 3
    toughness = 3

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{1}{B}"),
            Costs.SacrificeAnother(GameObjectFilter.Creature)
        )
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        description = "{1}{B}, Sacrifice another creature: Put a +1/+1 counter on this creature."
    }

    triggeredAbility {
        trigger = Triggers.a(
            GameObjectFilter.Creature.youControl().withCounter(CounterType.PLUS_ONE_PLUS_ONE)
        ).dies()
        effect = Effects.DrawCards(1)
        description = "Whenever a creature you control with a +1/+1 counter on it dies, draw a card."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "126"
        artist = "Simon Dominic"
        flavorText = "\"Perhaps its distant ancestors were radiant felidars, but no hint of their nobility " +
            "remains.\"\n—Akiri, kor line-slinger"
        imageUri = "https://cards.scryfall.io/normal/front/b/2/b27fcfc5-a1a9-4b21-8240-2ed6346c64e1.jpg"
    }
}
