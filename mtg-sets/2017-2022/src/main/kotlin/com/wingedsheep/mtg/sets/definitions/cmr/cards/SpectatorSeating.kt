package com.wingedsheep.mtg.sets.definitions.cmr.cards

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
 * Spectator Seating — Commander Legends #356 (canonical printing)
 * Land
 *
 * This land enters tapped unless you have two or more opponents.
 * {T}: Add {R} or {W}.
 *
 * The battlebond-land shape (Luxury Suite): the opponent count is the current one.
 */
val SpectatorSeating = card("Spectator Seating") {
    manaCost = ""
    colorIdentity = "RW"
    typeLine = "Land"
    oracleText = "This land enters tapped unless you have two or more opponents.\n{T}: Add {R} or {W}."

    replacementEffect(
        EntersTapped(
            unlessCondition = Conditions.CompareAmounts(
                DynamicAmounts.playerCount(Player.EachOpponent), ComparisonOperator.GTE, 2
            )
        )
    )

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.RED)
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
        collectorNumber = "356"
        artist = "Ravenna Tran"
        flavorText = "The loudest and most heartfelt cheers come from the cheap seats."
        imageUri = "https://cards.scryfall.io/normal/front/2/f/2f6f1453-fe93-4a29-965c-5f867a81e8b3.jpg?1783928740"
        ruling("2020-11-10", "Count the number of opponents you currently have, not how many you started with. If your four-player game is down to you and a single opponent, the land enters the battlefield tapped.")
        ruling("2020-11-10", "If an effect puts the land onto the battlefield tapped, having two or more opponents won't untap it.")
    }
}
