package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Remorseless Punishment
 * {3}{B}{B}
 * Sorcery
 *
 * Target opponent loses 5 life unless that player discards two cards or sacrifices a creature or
 * planeswalker of their choice. Repeat this process once.
 *
 * The process is one [Effects.PayOrSuffer] with a [Costs.pay.Choice] of the two ways out, run
 * twice by [Effects.Repeat]. The same targeted opponent answers both times, choosing afresh each
 * time. An option they can't take — fewer than two cards in hand, no creature or planeswalker — is
 * not offered; with neither, they lose 5 life twice.
 */
val RemorselessPunishment = card("Remorseless Punishment") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Target opponent loses 5 life unless that player discards two cards or sacrifices a " +
        "creature or planeswalker of their choice. Repeat this process once."

    spell {
        val opponent = target(Targets.Opponent)
        effect = Effects.Repeat(
            amount = DynamicAmounts.fixed(2),
            body = Effects.PayOrSuffer(
                cost = Costs.pay.Choice(
                    listOf(
                        Costs.pay.Discard(count = 2),
                        Costs.pay.Sacrifice(filter = GameObjectFilter.CreatureOrPlaneswalker)
                    )
                ),
                suffer = Effects.LoseLife(5, opponent),
                player = opponent,
                // Asked of the opponent, so the consequence is in their words.
                consequenceDescription = "lose 5 life"
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "89"
        artist = "Ryan Barger"
        flavorText = "\"I have suffered a thousand lifetimes of agony on this wretched world. I've far less " +
            "time to share with you, but I assure you, no less pain.\"\n—Ob Nixilis"
        imageUri = "https://cards.scryfall.io/normal/front/1/d/1dde9379-b335-400f-9748-a7ca3e293ee0.jpg?1783937911"
    }
}
