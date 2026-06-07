package com.wingedsheep.mtg.sets.definitions.tmp.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Altar of Dementia
 * {2}
 * Artifact
 *
 * Sacrifice a creature: Target player mills cards equal to the sacrificed creature's power.
 */
val AltarOfDementia = card("Altar of Dementia") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "Sacrifice a creature: Target player mills cards equal to the sacrificed creature's power."

    activatedAbility {
        cost = Costs.Sacrifice(GameObjectFilter.Creature)
        val player = target("player", Targets.Player)
        effect = Patterns.Library.mill(
            DynamicAmounts.sacrificedPower(),
            EffectTarget.BoundVariable("player")
        )
        description = "Sacrifice a creature: Target player mills cards equal to the sacrificed creature's power."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "276"
        artist = "Brom"
        imageUri = "https://cards.scryfall.io/normal/front/4/f/4f2da99f-3c53-4980-97d6-2158c765aac0.jpg?1562054217"
    }
}
