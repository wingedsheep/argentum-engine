package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Quandrix Pledgemage — Strixhaven: School of Mages #219 (canonical printing)
 * {1}{G/U}{G/U} · Creature — Merfolk Druid · 2/2
 *
 * Magecraft — Whenever you cast or copy an instant or sorcery spell, put a +1/+1 counter on this
 * creature.
 */
val QuandrixPledgemage = card("Quandrix Pledgemage") {
    manaCost = "{1}{G/U}{G/U}"
    colorIdentity = "GU"
    typeLine = "Creature — Merfolk Druid"
    power = 2
    toughness = 2
    oracleText = "Magecraft — Whenever you cast or copy an instant or sorcery spell, put a +1/+1 " +
        "counter on this creature."

    triggeredAbility {
        trigger = Triggers.you.castsOrCopies(GameObjectFilter.InstantOrSorcery)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        description = "Magecraft — Whenever you cast or copy an instant or sorcery spell, put a " +
            "+1/+1 counter on this creature."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "219"
        artist = "Caroline Gariba"
        flavorText = "\"The first step toward transcending physical limitations is discarding " +
            "assumptions about what is possible.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/7/07633b7f-4150-458b-89c3-d05dc0e3c4bd.jpg?1783927300"
        ruling("2021-04-16", "If an effect creates a copy of an instant or sorcery spell, this will also cause the magecraft ability to trigger.")
        ruling("2021-04-16", "If an effect creates multiple copies of an instant or sorcery spell, magecraft abilities trigger once for each copy created by the effect.")
    }
}
