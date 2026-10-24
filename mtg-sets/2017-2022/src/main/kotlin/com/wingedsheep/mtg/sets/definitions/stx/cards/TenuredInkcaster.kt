package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Tenured Inkcaster — Strixhaven: School of Mages #88 (canonical printing)
 * {4}{B} · Creature — Vampire Warlock · 2/2
 *
 * When this creature enters, put a +1/+1 counter on target creature.
 * Whenever a creature you control with a +1/+1 counter on it attacks, each opponent loses 1 life
 * and you gain 1 life.
 *
 * The attack trigger is Byrke, Long Ear of the Law's `withCounter` attacker filter; the payoff is
 * Bastion of Remembrance's each-opponent drain.
 */
val TenuredInkcaster = card("Tenured Inkcaster") {
    manaCost = "{4}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Vampire Warlock"
    power = 2
    toughness = 2
    oracleText = "When this creature enters, put a +1/+1 counter on target creature.\n" +
        "Whenever a creature you control with a +1/+1 counter on it attacks, each opponent loses " +
        "1 life and you gain 1 life."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter.Creature)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creature)
        description = "When this creature enters, put a +1/+1 counter on target creature."
    }

    triggeredAbility {
        trigger = Triggers.a(
            GameObjectFilter.Creature.youControl().withCounter(CounterType.PLUS_ONE_PLUS_ONE)
        ).attacks()
        effect = Effects.LoseLife(1, EffectTarget.PlayerRef(Player.EachOpponent)) then Effects.GainLife(1)
        description = "Whenever a creature you control with a +1/+1 counter on it attacks, each " +
            "opponent loses 1 life and you gain 1 life."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "88"
        artist = "Jake Murray"
        flavorText = "\"Ah, another future archmage for my collection.\""
        imageUri = "https://cards.scryfall.io/normal/front/9/2/92eba33e-efea-4e58-a832-1c6049fb211f.jpg?1783927361"
    }
}
