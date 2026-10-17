package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry

/**
 * March of Reckless Joy — Kamigawa: Neon Dynasty #154 (canonical printing)
 * {X}{R} · Instant
 *
 * As an additional cost to cast this spell, you may exile any number of red cards from your hand.
 * This spell costs {2} less to cast for each card exiled this way.
 * Exile the top X cards of your library. You may play up to two of those cards until the end of
 * your next turn.
 *
 * "Up to two" is a `maxPlays = 2` grant over the whole pile: each card played from it spends one
 * play, and the second revokes the permission for the rest.
 */
val MarchOfRecklessJoy = card("March of Reckless Joy") {
    manaCost = "{X}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "As an additional cost to cast this spell, you may exile any number of red cards " +
        "from your hand. This spell costs {2} less to cast for each card exiled this way.\n" +
        "Exile the top X cards of your library. You may play up to two of those cards until the end " +
        "of your next turn."

    additionalCost(Costs.additional.ExileAnyNumberFromHandForCostReduction(
        GameObjectFilter.Any.withColor(Color.RED), reductionPerUnit = 2
    ))

    spell {
        effect = Effects.Pipeline {
            val exiled = gather(CardSource.TopOfLibrary(DynamicAmounts.xValue()))
            move(exiled, CardDestination.ToZone(Zone.EXILE))
            run(Effects.GrantMayPlayFromExile(
                from = exiled,
                expiry = MayPlayExpiry.UntilEndOfNextTurn,
                maxPlays = 2
            ))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "154"
        artist = "Fiona Hsieh"
        imageUri = "https://cards.scryfall.io/normal/front/7/8/780e1bf1-e392-40f2-9e84-764dedc5fcd4.jpg?1783923863"
    }
}
