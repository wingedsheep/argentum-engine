package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Gixian Infiltrator
 * {1}{B}
 * Creature — Phyrexian Human
 * 2/1
 * Whenever you sacrifice another permanent, put a +1/+1 counter on this creature.
 */
val GixianInfiltrator = card("Gixian Infiltrator") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Phyrexian Human"
    power = 2
    toughness = 1
    oracleText = "Whenever you sacrifice another permanent, put a +1/+1 counter on this creature."

    triggeredAbility {
        trigger = Triggers.you.sacrificesAnother(GameObjectFilter.Permanent)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        description = "Whenever you sacrifice another permanent, put a +1/+1 counter on this creature."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "98"
        artist = "Peter Polach"
        flavorText = "\"Terisia City's fall is inevitable. My master's plan runs as sure and steady as the oil in my veins.\""
        imageUri = "https://cards.scryfall.io/normal/front/c/9/c94a3317-7d1f-4f29-8353-180f1ab48d18.jpg"
    }
}
