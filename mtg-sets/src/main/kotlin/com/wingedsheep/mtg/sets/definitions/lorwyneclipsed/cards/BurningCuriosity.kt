package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AdditionalCost
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.GrantMayPlayFromExileEffect
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Burning Curiosity
 * {2}{R}
 * Sorcery
 *
 * As an additional cost to cast this spell, you may blight 1.
 * (You may put a -1/-1 counter on a creature you control.)
 * Exile the top two cards of your library. If this spell's additional cost was paid,
 * exile the top three cards instead. Until the end of your next turn, you may play those cards.
 */
val BurningCuriosity = card("Burning Curiosity") {
    manaCost = "{2}{R}"
    typeLine = "Sorcery"
    oracleText = "As an additional cost to cast this spell, you may blight 1. " +
        "(You may put a -1/-1 counter on a creature you control.)\n" +
        "Exile the top two cards of your library. If this spell's additional cost was paid, " +
        "exile the top three cards instead. Until the end of your next turn, you may play those cards."

    additionalCost(AdditionalCost.BlightOrPay(blightAmount = 1, alternativeManaCost = ""))

    spell {
        effect = ConditionalEffect(
            condition = Conditions.BlightWasPaid,
            effect = exileTopAndMayPlay(3),
            elseEffect = exileTopAndMayPlay(2)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "129"
        artist = "Jim Pavelec"
        flavorText = "Cinders are drawn to any heat, at any cost."
        imageUri = "https://cards.scryfall.io/normal/front/6/8/689ed288-6228-4d7e-b198-56a12b8be299.jpg?1767732735"
        ruling(
            "2025-11-17",
            "You pay all costs and follow all timing rules for cards played this way. For example, " +
                "if an exiled card is a land card, you may play it only during your main phase while the stack is empty."
        )
    }
}

private fun exileTopAndMayPlay(count: Int): Effect = CompositeEffect(
    listOf(
        GatherCardsEffect(
            source = CardSource.TopOfLibrary(DynamicAmount.Fixed(count)),
            storeAs = "exiled"
        ),
        MoveCollectionEffect(
            from = "exiled",
            destination = CardDestination.ToZone(Zone.EXILE)
        ),
        GrantMayPlayFromExileEffect(
            from = "exiled",
            expiry = MayPlayExpiry.UntilEndOfNextTurn
        )
    )
)
