package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Plague Belcher
 * {2}{B}
 * Creature — Zombie Beast
 * 5/4
 * Menace
 * When this creature enters, put two -1/-1 counters on target creature you control.
 * Whenever another Zombie you control dies, each opponent loses 1 life.
 */
val PlagueBelcher = card("Plague Belcher") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie Beast"
    oracleText = "Menace\n" +
        "When this creature enters, put two -1/-1 counters on target creature you control.\n" +
        "Whenever another Zombie you control dies, each opponent loses 1 life."
    power = 5
    toughness = 4

    keywords(Keyword.MENACE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter.CreatureYouControl)
        effect = Effects.AddCounters(CounterType.MINUS_ONE_MINUS_ONE, 2, creature)
    }

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Permanent.withSubtype(Subtype.ZOMBIE).youControl()).dies()
        effect = Effects.LoseLife(1, EffectTarget.PlayerRef(Player.EachOpponent))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "104"
        artist = "Izzy"
        imageUri = "https://cards.scryfall.io/normal/front/2/8/280ae211-f025-4971-83e6-118ca08a1911.jpg?1783936500"
        ruling(
            "2017-04-18",
            "If another Zombie you control dies at the same time as Plague Belcher, Plague Belcher's last ability will trigger for that Zombie."
        )
    }
}
