package com.wingedsheep.mtg.sets.definitions.conflux.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.CollectionSlot
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator

/**
 * Martial Coup
 * {X}{W}{W}
 * Sorcery
 * Create X 1/1 white Soldier creature tokens. If X is 5 or more, destroy all other creatures.
 *
 * "Other" means other than the Soldiers this spell just made: the wipe gathers every creature on
 * the battlefield and subtracts [CollectionSlot.CreatedTokens] (the token step publishes its ids
 * there), so the new army survives while everything else — including your own older creatures —
 * is destroyed.
 */
val MartialCoup = card("Martial Coup") {
    manaCost = "{X}{W}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Create X 1/1 white Soldier creature tokens. If X is 5 or more, destroy all other creatures."

    spell {
        effect = Effects.CreateToken(
            count = DynamicAmounts.xValue(),
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Soldier")
        ) then Effects.If(
            condition = Conditions.CompareAmounts(DynamicAmounts.xValue(), ComparisonOperator.GTE, 5),
            then = Effects.Pipeline {
                val creatures = gather(GameObjectFilter.Creature)
                destroy(exclude(creatures, CollectionSlot.CreatedTokens))
            }
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "11"
        artist = "Greg Staples"
        flavorText = "Their war forgotten, the nations of Bant stood united in the face of a common threat."
        imageUri = "https://cards.scryfall.io/normal/front/4/2/4201385f-6f74-4e3d-aafb-0eff82cb24c1.jpg?1783942492"
    }
}
