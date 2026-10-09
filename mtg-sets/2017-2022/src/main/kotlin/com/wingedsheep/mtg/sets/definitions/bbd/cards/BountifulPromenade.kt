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
 * Bountiful Promenade — Battlebond #81 (canonical printing)
 * Land
 *
 * This land enters tapped unless you have two or more opponents.
 * {T}: Add {G} or {W}.
 *
 * The Battlebond "battlebond land" cycle. "Two or more opponents" counts the opponents you have
 * *now* (`PlayerCount(EachOpponent)` reads `GameState.getOpponents`, which drops players who have
 * lost), so a four-player game down to a single opponent sees it enter tapped.
 */
val BountifulPromenade = card("Bountiful Promenade") {
    manaCost = ""
    colorIdentity = "GW"
    typeLine = "Land"
    oracleText = "This land enters tapped unless you have two or more opponents.\n{T}: Add {G} or {W}."

    replacementEffect(
        EntersTapped(
            unlessCondition = Conditions.CompareAmounts(
                DynamicAmounts.playerCount(Player.EachOpponent), ComparisonOperator.GTE, 2
            )
        )
    )

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.GREEN)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.WHITE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "81"
        artist = "Jung Park"
        flavorText = "No pilgrimage to Valor's Reach is complete without a stroll through its celebrated shopping district."
        imageUri = "https://cards.scryfall.io/normal/front/2/1/21865ed6-5edd-41f4-9ae0-f501872d91dc.jpg?1783934849"
        ruling("2018-06-08", "If you began the game with two or more opponents but now only have one opponent left, these lands enter the battlefield tapped.")
    }
}
