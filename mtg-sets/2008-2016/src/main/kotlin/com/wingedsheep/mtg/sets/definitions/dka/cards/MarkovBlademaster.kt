package com.wingedsheep.mtg.sets.definitions.dka.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Markov Blademaster
 * {1}{R}{R}
 * Creature — Vampire Warrior
 * 1/1
 *
 * Double strike
 * Whenever this creature deals combat damage to a player, put a +1/+1 counter on it.
 *
 * Double strike deals combat damage in two steps, so an unblocked Blademaster triggers twice — the
 * counter from the first-strike step makes the regular-damage hit one bigger.
 */
val MarkovBlademaster = card("Markov Blademaster") {
    manaCost = "{1}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Vampire Warrior"
    power = 1
    toughness = 1
    oracleText = "Double strike (This creature deals both first-strike and regular combat damage.)\n" +
        "Whenever this creature deals combat damage to a player, put a +1/+1 counter on it."

    keywords(Keyword.DOUBLE_STRIKE)

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "96"
        artist = "Jana Schirmer & Johannes Voss"
        flavorText = "\"Mortals practice swordplay for a few decades at best. How can they ever attain the exquisite mastery that eternity has to offer?\""
        imageUri = "https://cards.scryfall.io/normal/front/1/2/122163dd-e070-48af-8036-e9850541d138.jpg?1783940816"
    }
}
