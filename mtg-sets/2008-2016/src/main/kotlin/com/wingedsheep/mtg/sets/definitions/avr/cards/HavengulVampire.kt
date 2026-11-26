package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Havengul Vampire — Avacyn Restored #139
 * {3}{R} · Creature — Vampire · 2/2
 *
 * Whenever this creature deals combat damage to a player, put a +1/+1 counter on it.
 * Whenever another creature dies, put a +1/+1 counter on this creature.
 */
val HavengulVampire = card("Havengul Vampire") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Vampire"
    power = 2
    toughness = 2
    oracleText = "Whenever this creature deals combat damage to a player, put a +1/+1 counter on it.\n" +
        "Whenever another creature dies, put a +1/+1 counter on this creature."

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature).dies()
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "139"
        artist = "James Ryman"
        flavorText = "\"That slowly decreasing pulse—that's the beat I waltz to.\""
        imageUri = "https://cards.scryfall.io/normal/front/c/b/cbc09839-1463-40b8-86bd-fb96797b2633.jpg?1783940684"
    }
}
