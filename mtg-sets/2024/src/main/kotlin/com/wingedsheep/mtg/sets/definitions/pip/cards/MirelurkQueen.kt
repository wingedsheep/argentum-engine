package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

val MirelurkQueen = card("Mirelurk Queen") {
    manaCost = "{4}{U}"
    typeLine = "Creature — Crab Mutant"
    power = 4
    toughness = 4
    oracleText = "Vigilance\n" +
        "When this creature enters, target player gets two rad counters.\n" +
        "Whenever one or more nonland cards are milled, draw a card, then put a +1/+1 counter on this creature. This ability triggers only once each turn."

    keywords(Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val player = target(Targets.Player)
        effect = Effects.AddCounters(CounterType.RAD, 2, player)
    }

    triggeredAbility {
        trigger = Triggers.anyPlayer.mills(GameObjectFilter.Nonland, batch = true)
        oncePerTurn = true
        effect = Effects.DrawCards(1) then
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "33"
        artist = "Nicholas Gregory"
        imageUri = "https://cards.scryfall.io/normal/front/6/e/6e6d1843-9e05-4cdd-ad4e-15d7913d87f7.jpg?1783912412"
    }
}
