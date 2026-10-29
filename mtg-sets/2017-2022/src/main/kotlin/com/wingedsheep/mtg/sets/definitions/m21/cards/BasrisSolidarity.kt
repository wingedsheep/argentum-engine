package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Basri's Solidarity
 * {1}{W}
 * Sorcery
 *
 * Put a +1/+1 counter on each creature you control.
 */
val BasrisSolidarity = card("Basri's Solidarity") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Put a +1/+1 counter on each creature you control."

    spell {
        effect = Effects.ForEachInGroup(
            GroupFilter.AllCreaturesYouControl,
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.IterationEntity),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "10"
        artist = "Paul Scott Canavan"
        flavorText = "\"The will to fight and act as one is not limited to a single world. Oketra's teachings are universal.\"\n—Basri Ket"
        imageUri = "https://cards.scryfall.io/normal/front/c/2/c2e6fdc0-bdd4-4bba-b8f1-bbc8dfad038e.jpg?1783930746"
    }
}
