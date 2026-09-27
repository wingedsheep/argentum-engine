package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Rampaging Geoderm — March of the Machine #251
 * {2}{R}{G} · Creature — Dinosaur Beast · 3/3
 *
 * Trample, haste
 * Whenever you attack, target attacking creature gets +1/+1 until end of turn. If it's attacking a
 * battle, put a +1/+1 counter on it instead.
 *
 * "Instead" is an if/otherwise over the target's defender, read on resolution through
 * `attackingABattle()`.
 */
val RampagingGeoderm = card("Rampaging Geoderm") {
    manaCost = "{2}{R}{G}"
    colorIdentity = "RG"
    typeLine = "Creature — Dinosaur Beast"
    oracleText = "Trample, haste\n" +
        "Whenever you attack, target attacking creature gets +1/+1 until end of turn. If it's " +
        "attacking a battle, put a +1/+1 counter on it instead."
    power = 3
    toughness = 3

    keywords(Keyword.TRAMPLE, Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.you.attacks()
        val attacker = target(TargetFilter.AttackingCreature)
        effect = Effects.If(
            condition = Conditions.TargetMatchesFilter(GameObjectFilter.Creature.attackingABattle(), attacker),
            then = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, attacker),
            otherwise = Effects.ModifyStats(1, 1, attacker)
        )
        description = "Whenever you attack, target attacking creature gets +1/+1 until end of turn. " +
            "If it's attacking a battle, put a +1/+1 counter on it instead."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "251"
        artist = "Randy Vargas"
        flavorText = "The Phyrexian juggernaut found itself crushed between a rock and a hard place."
        imageUri = "https://cards.scryfall.io/normal/front/a/9/a973e70f-9e3e-47a3-94b9-7bb8a198a438.jpg?1783916939"
    }
}
