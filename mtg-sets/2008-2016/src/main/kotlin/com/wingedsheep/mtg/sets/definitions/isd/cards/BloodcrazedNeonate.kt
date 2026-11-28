package com.wingedsheep.mtg.sets.definitions.isd.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.MustAttack
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Bloodcrazed Neonate
 * {1}{R}
 * Creature — Vampire
 * 2/1
 * This creature attacks each combat if able.
 * Whenever this creature deals combat damage to a player, put a +1/+1 counter on it.
 */
val BloodcrazedNeonate = card("Bloodcrazed Neonate") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Vampire"
    oracleText = "This creature attacks each combat if able.\nWhenever this creature deals combat damage to a player, put a +1/+1 counter on it."
    power = 2
    toughness = 1
    staticAbility {
        ability = MustAttack()
    }
    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "131"
        artist = "Cynthia Sheppard"
        flavorText = "While elder vampires select their meals with care, the newly sired frenzy at the first whiff."
        imageUri = "https://cards.scryfall.io/normal/front/6/8/68d2452e-309d-44ae-9360-9d6e22a15e2b.jpg"
    }
}
