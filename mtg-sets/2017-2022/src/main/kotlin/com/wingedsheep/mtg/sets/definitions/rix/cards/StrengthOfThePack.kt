package com.wingedsheep.mtg.sets.definitions.rix.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Strength of the Pack
 * {4}{G}{G}
 * Sorcery
 *
 * Put two +1/+1 counters on each creature you control.
 */
val StrengthOfThePack = card("Strength of the Pack") {
    manaCost = "{4}{G}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Put two +1/+1 counters on each creature you control."

    spell {
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.youControl()),
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 2, EffectTarget.IterationEntity)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "145"
        artist = "Tomasz Jedruszek"
        flavorText = "The pack that ambushes, eviscerates, and devours together stays together."
        imageUri = "https://cards.scryfall.io/normal/front/1/5/15dd7d94-e2bd-4320-8452-9e0deb1be351.jpg?1783935283"
    }
}
