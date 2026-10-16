package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Feral Ghoul — the dies trigger reads the Ghoul's power as it last existed on the battlefield
 * (2024-03-08 ruling), so +1/+1 counters it grew from earlier deaths count. `ForEachPlayer`
 * rebinds `Controller` to each opponent in turn while `sourcePower()` still reads the Ghoul.
 */
val FeralGhoul = card("Feral Ghoul") {
    manaCost = "{2}{B}"
    typeLine = "Creature — Zombie Mutant"
    power = 2
    toughness = 2
    oracleText = "Menace\n" +
        "Whenever another creature you control dies, put a +1/+1 counter on this creature.\n" +
        "When this creature dies, each opponent gets a number of rad counters equal to its power."

    keywords(Keyword.MENACE)

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature.youControl()).dies()
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.ForEachPlayer(
            Player.EachOpponent,
            Effects.AddDynamicCounters(CounterType.RAD, DynamicAmounts.sourcePower(), EffectTarget.Controller)
        )
        description = "When this creature dies, each opponent gets a number of rad counters equal to its power."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "44"
        artist = "Sergio Cosmai"
        imageUri = "https://cards.scryfall.io/normal/front/f/b/fbdef99d-6ce9-495d-a55b-e6ba799d3da7.jpg?1783912408"
        ruling(
            "2024-03-08",
            "Use Feral Ghoul's power as it last existed on the battlefield to determine how many rad counters each opponent gets."
        )
    }
}
