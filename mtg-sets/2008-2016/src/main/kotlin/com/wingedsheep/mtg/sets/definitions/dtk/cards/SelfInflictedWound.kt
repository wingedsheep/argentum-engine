package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator

/**
 * Self-Inflicted Wound
 * {1}{B}
 * Sorcery
 * Target opponent sacrifices a green or white creature of their choice. If that player does,
 * they lose 2 life.
 *
 * "If that player does" reads [DynamicAmounts.permanentsSacrificedThisWay] off the edict in the
 * same composite (the Woebringer Demon / Reweave shape), so an opponent with no green or white
 * creature loses no life.
 */
val SelfInflictedWound = card("Self-Inflicted Wound") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Target opponent sacrifices a green or white creature of their choice. " +
        "If that player does, they lose 2 life."

    spell {
        val opponent = target(Targets.Opponent)
        effect = Effects.Sacrifice(
            GameObjectFilter.Creature.withAnyColor(Color.GREEN, Color.WHITE),
            1,
            opponent
        ) then
            Effects.If(
                condition = Conditions.CompareAmounts(
                    DynamicAmounts.permanentsSacrificedThisWay(),
                    ComparisonOperator.GTE,
                    1
                ),
                then = Effects.LoseLife(2, opponent)
            )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "117"
        artist = "Mathias Kollros"
        flavorText = "\"Worse than watching the cruelest deed is watching it done by your own hand.\"\n—Baihir, Dromoka mage"
        imageUri = "https://cards.scryfall.io/normal/front/f/6/f69b1a5d-fb17-43f0-bf29-4ade853e0e6e.jpg?1783938594"
    }
}
