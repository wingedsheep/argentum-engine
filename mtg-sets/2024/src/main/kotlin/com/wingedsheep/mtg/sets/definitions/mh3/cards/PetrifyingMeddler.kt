package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Petrifying Meddler
 * {4}{U}
 * Creature — Eldrazi
 * 4/5
 * Devoid
 * When you cast this spell, tap up to one target creature and put a stun counter on it.
 * Reach
 */
val PetrifyingMeddler = card("Petrifying Meddler") {
    manaCost = "{4}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Eldrazi"
    power = 4
    toughness = 5
    oracleText = "Devoid (This card has no color.)\nWhen you cast this spell, tap up to one target creature and put a stun counter on it. (If a permanent with a stun counter would become untapped, remove one from it instead.)\nReach"

    keywords(Keyword.DEVOID, Keyword.REACH)

    triggeredAbility {
        trigger = Triggers.self.isCast()
        val creature = target(TargetFilter.Creature, optional = true)
        effect = Effects.Tap(creature) then Effects.AddCounters(CounterType.STUN, 1, creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "66"
        artist = "Mathias Kollros"
        imageUri = "https://cards.scryfall.io/normal/front/f/0/f0f2bdfd-1cbc-456e-aba7-4e0b6485cf8a.jpg?1783911290"
    }
}
