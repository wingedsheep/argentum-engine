package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.AttackPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Makeshift Battalion
 * {2}{W}
 * Creature — Human Soldier
 * 3/2
 * Battalion — Whenever this creature and at least two other creatures attack, put a +1/+1 counter
 * on this creature.
 *
 * Battalion is the self attack trigger with `AttackerCountAtLeast(3)` (Paladin Elizabeth Taggerdy).
 */
val MakeshiftBattalion = card("Makeshift Battalion") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Soldier"
    power = 3
    toughness = 2
    oracleText = "Battalion — Whenever this creature and at least two other creatures attack, put a +1/+1 " +
        "counter on this creature."

    triggeredAbility {
        trigger = Triggers.self.attacks(setOf(AttackPredicate.AttackerCountAtLeast(3)))
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "22"
        artist = "Zoltan Boros"
        flavorText = "Their ranks are thin, their formation is improvised, their weapons are borrowed, and " +
            "you'd best get out of their way."
        imageUri = "https://cards.scryfall.io/normal/front/8/2/82293d58-d3b4-4cf6-8a16-32ae3e509696.jpg?1783933479"
        ruling(
            "2024-01-12",
            "Makeshift Battalion and the other attacking creatures don't have to be attacking the same player, " +
                "planeswalker, or battle.",
        )
        ruling(
            "2020-06-23",
            "Once Makeshift Battalion's ability has triggered, it doesn't matter how many creatures are still " +
                "attacking when that ability resolves.",
        )
    }
}
