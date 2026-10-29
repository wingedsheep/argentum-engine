package com.wingedsheep.mtg.sets.definitions.cmr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Thalisse, Reverent Medium — Commander Legends #291
 * {3}{W}{B} · Legendary Creature — Human Cleric · 3/4
 * At the beginning of each end step, create X 1/1 white Spirit creature tokens with flying, where X
 * is the number of tokens you created this turn.
 *
 * X is `DynamicAmounts.tokensCreatedThisTurn()`, read as the trigger resolves: every token you
 * created this turn, gone or given away, but not a permanent-spell copy that resolved into a token.
 */
val ThalisseReverentMedium = card("Thalisse, Reverent Medium") {
    manaCost = "{3}{W}{B}"
    colorIdentity = "WB"
    typeLine = "Legendary Creature — Human Cleric"
    power = 3
    toughness = 4
    oracleText = "At the beginning of each end step, create X 1/1 white Spirit creature tokens with flying, " +
        "where X is the number of tokens you created this turn."

    triggeredAbility {
        trigger = Triggers.anyPlayer.beginningOf(Step.END)
        effect = Effects.CreateToken(
            count = DynamicAmounts.tokensCreatedThisTurn(),
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Spirit"),
            keywords = setOf(Keyword.FLYING),
            imageUri = "https://cards.scryfall.io/normal/front/2/1/21e580cb-6092-48b7-ba3e-d64ec28860da.jpg?1783928591",
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "291"
        artist = "Heonhwa"
        flavorText = "She hears every voice from the swirling, whispering mist and swears to tell their stories."
        imageUri = "https://cards.scryfall.io/normal/front/4/7/474d6363-ebe1-4a1a-b4e6-7bd53d878527.jpg?1783928769"
        ruling(
            "2020-11-10",
            "Thalisse's ability counts all tokens you created that turn prior to the ability resolving, including " +
                "both noncreature tokens and creature tokens. It doesn't matter if those tokens are still on the " +
                "battlefield or still under your control."
        )
        ruling(
            "2020-11-10",
            "If an effect creates a copy of a permanent spell, that spell becomes a token on the battlefield under " +
                "your control, but that token has not been \"created.\" It won't count for Thalisse's ability."
        )
    }
}
