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
 * Sea of Clouds — Battlebond #84 (canonical printing)
 * Land
 *
 * This land enters tapped unless you have two or more opponents.
 * {T}: Add {W} or {U}.
 *
 * The Battlebond "battlebond land" cycle. "Two or more opponents" counts the opponents you have
 * *now* (`PlayerCount(EachOpponent)` reads `GameState.getOpponents`, which drops players who have
 * lost), so a four-player game down to a single opponent sees it enter tapped.
 */
val SeaOfClouds = card("Sea of Clouds") {
    manaCost = ""
    colorIdentity = "WU"
    typeLine = "Land"
    oracleText = "This land enters tapped unless you have two or more opponents.\n{T}: Add {W} or {U}."

    replacementEffect(
        EntersTapped(
            unlessCondition = Conditions.CompareAmounts(
                DynamicAmounts.playerCount(Player.EachOpponent), ComparisonOperator.GTE, 2
            )
        )
    )

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.WHITE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.BLUE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "84"
        artist = "Florian de Gesincourt"
        flavorText = "Skybridges from across Kylem converge at the grand stadium of Valor's Reach."
        imageUri = "https://cards.scryfall.io/normal/front/0/8/080aebe8-535e-4632-b733-8aba98abff22.jpg?1783934847"
        ruling("2018-06-08", "If you began the game with two or more opponents but now only have one opponent left, these lands enter the battlefield tapped.")
    }
}
