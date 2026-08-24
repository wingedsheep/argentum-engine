package com.wingedsheep.mtg.sets.definitions.s99.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Squall
 * {2}{G}
 * Sorcery
 * Squall deals 2 damage to each creature with flying.
 *
 * Hurricane without the player half: one [Effects.ForEachInGroup] over the flying creatures,
 * with `EffectTarget.Self` naming the current iteration entity.
 */
val Squall = card("Squall") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Squall deals 2 damage to each creature with flying."

    spell {
        effect = Effects.ForEachInGroup(
            GroupFilter.AllCreatures.withKeyword(Keyword.FLYING),
            Effects.DealDamage(2, EffectTarget.Self)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "143"
        artist = "Carl Critchlow"
        flavorText = "\"To-night the winds begin to rise . . . The rooks are blown about the skies . . . .\"\n" +
            "—Alfred, Lord Tennyson, *In Memoriam*"
        imageUri = "https://cards.scryfall.io/normal/front/6/3/63c1b2f6-e47f-4f18-a94a-1d08eb009ef3.jpg"
    }
}
