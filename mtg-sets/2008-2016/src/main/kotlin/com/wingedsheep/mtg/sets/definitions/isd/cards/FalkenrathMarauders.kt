package com.wingedsheep.mtg.sets.definitions.isd.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Falkenrath Marauders
 * {3}{R}{R}
 * Creature — Vampire Warrior
 * 2/2
 *
 * Flying
 * Haste
 * Whenever this creature deals combat damage to a player, put two +1/+1 counters on it.
 */
val FalkenrathMarauders = card("Falkenrath Marauders") {
    manaCost = "{3}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Vampire Warrior"
    power = 2
    toughness = 2
    oracleText = "Flying\n" +
        "Haste (This creature can attack and {T} as soon as it comes under your control.)\n" +
        "Whenever this creature deals combat damage to a player, put two +1/+1 counters on it."

    keywords(Keyword.FLYING, Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 2, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "141"
        artist = "James Ryman"
        flavorText = "No slinking in the dark for them, no luring whispers. They claim their prey in the speed and glory of the hunt."
        imageUri = "https://cards.scryfall.io/normal/front/b/9/b9c09887-6d2b-48b4-a483-16b8a45babd0.jpg?1783940937"
    }
}
