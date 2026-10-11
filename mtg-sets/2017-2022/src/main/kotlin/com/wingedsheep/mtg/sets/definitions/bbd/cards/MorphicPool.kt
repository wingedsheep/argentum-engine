package com.wingedsheep.mtg.sets.definitions.bbd.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Morphic Pool — Battlebond #83 (canonical printing)
 * Land
 *
 * This land enters tapped unless you have two or more opponents.
 * {T}: Add {U} or {B}.
 *
 * The Battlebond "battlebond land" cycle. "Two or more opponents" counts the opponents you have
 * *now* (`PlayerCount(EachOpponent)` reads `GameState.getOpponents`, which drops players who have
 * lost), so a four-player game down to a single opponent sees it enter tapped.
 */
val MorphicPool = card("Morphic Pool") {
    manaCost = ""
    colorIdentity = "UB"
    typeLine = "Land"
    oracleText = "This land enters tapped unless you have two or more opponents.\n{T}: Add {U} or {B}."

    replacementEffect(
        EntersTapped(
            unlessCondition = Conditions.CompareAmounts(
                DynamicAmounts.playerCount(Player.EachOpponent), ComparisonOperator.GTE, 2
            )
        )
    )

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.BLUE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.BLACK)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "83"
        artist = "Grzegorz Rutkowski"
        flavorText = "Ancient magic reshapes the stage into a variety of landscapes."
        imageUri = "https://cards.scryfall.io/normal/front/6/3/63b9efd6-0709-4d4b-a907-d8a77ec1a327.jpg?1783934849"
        ruling("2018-06-08", "If you began the game with two or more opponents but now only have one opponent left, these lands enter the battlefield tapped.")
    }
}
