package com.wingedsheep.mtg.sets.definitions.edgeofeternities.cards

import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Lost in Space
 * {3}{U}
 * Instant
 * Target artifact or creature's owner puts it on their choice of the top or bottom of their library. Surveil 1. (Look at the top card of your library. You may put it into your graveyard.)
 */
val LostInSpace = card("Lost in Space") {
    manaCost = "{3}{U}"
    typeLine = "Instant"
    oracleText = "Target artifact or creature's owner puts it on their choice of the top or bottom of their library. Surveil 1. (Look at the top card of your library. You may put it into your graveyard.)"

    spell {
        val target = target("target artifact or creature", TargetPermanent(filter = TargetFilter(GameObjectFilter.Artifact or GameObjectFilter.Creature)))
        effect = Effects.Composite(
            listOf(
                Effects.PutOnTopOrBottomOfLibrary(target),
                EffectPatterns.surveil(1)
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "62"
        artist = "Allen Panakal"
        flavorText = "Every so often, the Edge reminds you that there's always more to discover."
        imageUri = "https://cards.scryfall.io/normal/front/6/d/6d9d7979-97af-4c85-86f5-1b3704f74e8b.jpg?1752946795"
    }
}
