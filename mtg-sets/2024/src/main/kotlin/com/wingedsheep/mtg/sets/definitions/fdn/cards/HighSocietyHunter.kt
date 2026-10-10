package com.wingedsheep.mtg.sets.definitions.fdn.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * High-Society Hunter
 * {3}{B}{B}
 * Creature — Vampire Noble
 * 5/3
 *
 * Flying
 * Whenever this creature attacks, you may sacrifice another creature.
 * If you do, put a +1/+1 counter on this creature.
 * Whenever another nontoken creature dies, draw a card.
 */
val HighSocietyHunter = card("High-Society Hunter") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Vampire Noble"
    power = 5
    toughness = 3
    oracleText = "Flying\n" +
        "Whenever this creature attacks, you may sacrifice another creature. " +
        "If you do, put a +1/+1 counter on this creature.\n" +
        "Whenever another nontoken creature dies, draw a card."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.MayPay(
            Effects.SacrificeOwn(GameObjectFilter.Creature, excludeSource = true),
            then = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        )
        description = "Whenever this creature attacks, you may sacrifice another creature. " +
            "If you do, put a +1/+1 counter on this creature."
    }

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature.nontoken()).dies()
        effect = Effects.DrawCards(1)
        description = "Whenever another nontoken creature dies, draw a card."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "61"
        artist = "Daneen Wilkerson"
        flavorText = "\"You've been a marvelous servant. Consider this your final assignment.\""
        imageUri = "https://cards.scryfall.io/normal/front/5/1/51da4a4b-ea12-4169-a7cf-eb4427f13e84.jpg?1782689213"
    }
}
