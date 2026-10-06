package com.wingedsheep.mtg.sets.definitions.c19.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry

/**
 * Ignite the Future
 * {3}{R}
 * Sorcery
 *
 * Exile the top three cards of your library. Until the end of your next turn, you may play those
 * cards. If this spell was cast from a graveyard, you may play cards this way without paying their
 * mana costs.
 * Flashback {7}{R}
 *
 * Impulse draw over the stored collection, then — only on a graveyard (flashback) cast — the free
 * waiver over the same cards with the same `UntilEndOfNextTurn` expiry, so "this way" lasts exactly
 * as long as the permission does.
 */
val IgniteTheFuture = card("Ignite the Future") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Exile the top three cards of your library. Until the end of your next turn, you may play those cards. " +
        "If this spell was cast from a graveyard, you may play cards this way without paying their mana costs.\n" +
        "Flashback {7}{R} (You may cast this card from your graveyard for its flashback cost. Then exile it.)"

    spell {
        effect = Patterns.Exile.impulse(count = 3, expiry = MayPlayExpiry.UntilEndOfNextTurn) then
            Effects.If(
                condition = Conditions.WasCastFromGraveyard,
                then = Effects.GrantPlayWithoutPayingCost("impulseExiled", MayPlayExpiry.UntilEndOfNextTurn)
            )
    }

    keywordAbility(KeywordAbility.flashback("{7}{R}"))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "27"
        artist = "Alex Konstad"
        imageUri = "https://cards.scryfall.io/normal/front/5/a/5a9472e0-1a6d-47f7-91eb-9bdb000e1f1b.jpg?1783932806"
    }
}
