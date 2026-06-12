package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostGating
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.conditions.Compare
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Lashwhip Predator
 * {4}{G}{G}
 * Creature — Plant Beast
 * This spell costs {2} less to cast if your opponents control three or more creatures.
 * Reach
 * 5/7
 */
val LashwhipPredator = card("Lashwhip Predator") {
    manaCost = "{4}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Plant Beast"
    power = 5
    toughness = 7
    oracleText = "This spell costs {2} less to cast if your opponents control three or more creatures.\nReach"

    keywords(Keyword.REACH)

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGeneric(2),
            gating = CostGating.OnlyIf(
                Compare(
                    DynamicAmount.AggregateBattlefield(Player.EachOpponent, GameObjectFilter.Creature),
                    ComparisonOperator.GTE,
                    DynamicAmount.Fixed(3),
                ),
            ),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "195"
        artist = "Brian Valeza"
        flavorText = "\"Oh, look at the size of those petals! That one's eaten well this season.\"\n—Bez, Pinnacle nature surveyor"
        imageUri = "https://cards.scryfall.io/normal/front/2/4/24553e98-29a9-47e3-91c7-9add708d9ad1.jpg?1752947350"
    }
}
