package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Glistening Deluge
 * {1}{B}{B}
 * Sorcery
 * All creatures get -1/-1 until end of turn. Creatures that are green and/or white get an
 * additional -2/-2 until end of turn.
 */
val GlisteningDeluge = card("Glistening Deluge") {
    manaCost = "{1}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "All creatures get -1/-1 until end of turn. Creatures that are green and/or white get an additional -2/-2 until end of turn."

    spell {
        effect = Effects.ForEachInGroup(
            filter = GroupFilter.AllCreatures,
            effect = Effects.ModifyStats(-1, -1, EffectTarget.IterationEntity)
        ) then Effects.ForEachInGroup(
            filter = GroupFilter(
                GameObjectFilter.Creature.withColor(Color.GREEN) or
                    GameObjectFilter.Creature.withColor(Color.WHITE)
            ),
            effect = Effects.ModifyStats(-2, -2, EffectTarget.IterationEntity)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "107"
        artist = "Anastasia Balakchina"
        flavorText = "As oil began to pour from Ikoria's crystals, some monsters developed strange mechanical mutations. Others were simply pulled under."
        imageUri = "https://cards.scryfall.io/normal/front/8/3/83ca46ac-0698-4651-940d-3fd20c266b74.jpg?1783917009"
    }
}
