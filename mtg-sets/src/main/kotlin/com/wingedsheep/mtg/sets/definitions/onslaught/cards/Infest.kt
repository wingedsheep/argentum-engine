package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.effects.ForEachInGroupEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.effects.ModifyStatsEffect

/**
 * Infest
 * {1}{B}{B}
 * Sorcery
 * All creatures get -2/-2 until end of turn.
 */
val Infest = card("Infest") {
    manaCost = "{1}{B}{B}"
    typeLine = "Sorcery"
    oracleText = "All creatures get -2/-2 until end of turn."

    spell {
        effect = ForEachInGroupEffect(
            filter = GroupFilter.AllCreatures,
            effect = ModifyStatsEffect(-2, -2, EffectTarget.Self)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "157"
        artist = "Ben Thompson"
        flavorText = "\"This is the end for you, insect. And for you. And you.\"\n—Braids, dementia summoner"
        imageUri = "https://cards.scryfall.io/large/front/b/7/b7890ba2-aa42-4c8d-bbc1-94fb1d4150fc.jpg?1562938305"
    }
}
