package com.wingedsheep.mtg.sets.definitions.isd.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Stromkirk Patrol
 * {4}{B}
 * Creature — Vampire Soldier
 * 4/3
 * Whenever this creature deals combat damage to a player, put a +1/+1 counter on it.
 */
val StromkirkPatrol = card("Stromkirk Patrol") {
    manaCost = "{4}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Vampire Soldier"
    oracleText = "Whenever this creature deals combat damage to a player, put a +1/+1 counter on it."
    power = 4
    toughness = 3
    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "118"
        artist = "Karl Kopinski"
        flavorText = "\"You'd best move along, human. Our palates are far less discerning than our master's.\""
        imageUri = "https://cards.scryfall.io/normal/front/d/8/d86634a1-7016-4500-8857-924d51857bad.jpg"
    }
}
