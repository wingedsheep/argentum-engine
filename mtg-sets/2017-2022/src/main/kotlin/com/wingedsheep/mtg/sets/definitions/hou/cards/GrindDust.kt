package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardLayout
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Grind // Dust — Hour of Devastation #155
 *
 * Grind {1}{B} — Sorcery: Put a -1/-1 counter on each of up to two target creatures.
 * Dust {3}{W} — Sorcery: Aftermath (Cast this spell only from your graveyard. Then exile it.)
 *   Exile any number of target creatures that have -1/-1 counters on them.
 */
val GrindDust = card("Grind // Dust") {
    layout = CardLayout.SPLIT
    colorIdentity = "WB"

    face("Grind") {
        manaCost = "{1}{B}"
        typeLine = "Sorcery"
        oracleText = "Put a -1/-1 counter on each of up to two target creatures."

        spell {
            targets(TargetFilter.Creature, count = 2, optional = true)
            effect = Effects.ForEachTarget(
                Effects.AddCounters(CounterType.MINUS_ONE_MINUS_ONE, 1, EffectTarget.ContextTarget(0))
            )
        }
    }

    face("Dust") {
        manaCost = "{3}{W}"
        typeLine = "Sorcery"
        oracleText = "Aftermath (Cast this spell only from your graveyard. Then exile it.)\n" +
            "Exile any number of target creatures that have -1/-1 counters on them."
        keywords(Keyword.AFTERMATH)

        spell {
            targets(
                TargetFilter(GameObjectFilter.Creature.withCounter(CounterType.MINUS_ONE_MINUS_ONE)),
                unlimited = true
            )
            effect = Effects.ForEachTarget(Effects.Exile(EffectTarget.ContextTarget(0)))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "155"
        artist = "Josh Hass"
        imageUri = "https://cards.scryfall.io/normal/front/0/3/0383401f-d453-4e8f-82d2-5c016acc2591.jpg?1783936006"
        ruling("2017-07-14", "You can't target the same creature twice with Grind to give it two -1/-1 counters.")
        ruling("2017-07-14", "If one of Dust's target creatures loses its -1/-1 counters, leaves the battlefield, or otherwise becomes an illegal target before the spell resolves, it won't be exiled, but the remaining legal targets will be exiled.")
        ruling("2017-04-18", "A spell with aftermath cast from a graveyard will always be exiled afterward, whether it resolves, it's countered, or it leaves the stack in some other way.")
    }
}
