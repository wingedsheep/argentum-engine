package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.ninjutsu
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Coiling Stalker — Kamigawa: Neon Dynasty #179 (canonical printing)
 * {1}{G} · Creature — Snake Ninja · 2/1
 *
 * Ninjutsu {1}{G}
 * Whenever this creature deals combat damage to a player, put a +1/+1 counter on target creature
 * you control that doesn't have a +1/+1 counter on it.
 *
 * The "doesn't have a +1/+1 counter" restriction is a targeting requirement, so it is checked on
 * the trigger going on the stack and again on resolution (the counter fizzles if the target picked
 * up a +1/+1 counter in between). The Snake itself is a legal target the first time it connects.
 */
val CoilingStalker = card("Coiling Stalker") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Snake Ninja"
    power = 2
    toughness = 1
    oracleText = "Ninjutsu {1}{G} ({1}{G}, Return an unblocked attacker you control to hand: Put " +
        "this card onto the battlefield from your hand tapped and attacking.)\n" +
        "Whenever this creature deals combat damage to a player, put a +1/+1 counter on target " +
        "creature you control that doesn't have a +1/+1 counter on it."

    ninjutsu("{1}{G}")

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        val t = target(
            TargetFilter(
                GameObjectFilter.Creature.youControl().withoutCounter(CounterType.PLUS_ONE_PLUS_ONE)
            )
        )
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, t)
        description = "Whenever this creature deals combat damage to a player, put a +1/+1 counter " +
            "on target creature you control that doesn't have a +1/+1 counter on it."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "179"
        artist = "Svetlin Velinov"
        imageUri = "https://cards.scryfall.io/normal/front/6/e/6eba24f6-8a9c-4398-b2ab-3924b107c7b7.jpg?1783923853"
    }
}
