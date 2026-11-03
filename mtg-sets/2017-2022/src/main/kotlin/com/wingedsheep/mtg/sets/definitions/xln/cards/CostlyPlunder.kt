package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Costly Plunder
 * {1}{B}
 * Instant
 * As an additional cost to cast this spell, sacrifice an artifact or creature.
 * Draw two cards.
 */
val CostlyPlunder = card("Costly Plunder") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "As an additional cost to cast this spell, sacrifice an artifact or creature.\nDraw two cards."

    additionalCost(Costs.additional.SacrificePermanent(GameObjectFilter.Artifact.or(GameObjectFilter.Creature)))

    spell {
        effect = Effects.DrawCards(2)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "96"
        artist = "Ben Maier"
        flavorText = "No lock of iron is a match for Vraska's will of stone."
        imageUri = "https://cards.scryfall.io/normal/front/c/3/c3f1f41e-98fc-4f6b-b287-c8899dff8ab0.jpg?1783935765"
    }
}
