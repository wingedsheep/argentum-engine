package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Make Obsolete
 * {2}{B}
 * Instant
 * Creatures your opponents control get -1/-1 until end of turn.
 */
val MakeObsolete = card("Make Obsolete") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Creatures your opponents control get -1/-1 until end of turn."

    spell {
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.opponentControls()),
            Effects.ModifyStats(-1, -1, EffectTarget.IterationEntity),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "89"
        artist = "Darek Zabrocki"
        flavorText = "Yesterday's marvels are today's antiques."
        imageUri = "https://cards.scryfall.io/normal/front/e/0/e0a96feb-accc-4c30-8ecd-7d9272ebd45b.jpg?1783937205"
    }
}
