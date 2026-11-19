package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Fated Infatuation — Born of the Gods #39 (canonical / earliest real printing)
 * {U}{U}{U} · Instant
 *
 * Create a token that's a copy of target creature you control. If it's your turn, scry 2.
 *
 * Cackling Counterpart's [Effects.CreateTokenCopyOfTarget] followed by Fated Retribution's
 * "If it's your turn, scry 2" tail, checked at resolution.
 */
val FatedInfatuation = card("Fated Infatuation") {
    manaCost = "{U}{U}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Create a token that's a copy of target creature you control. If it's your turn, scry 2. " +
        "(Look at the top two cards of your library, then put any number of them on the bottom " +
        "of your library and the rest on top in any order.)"

    spell {
        val t = target(TargetFilter.CreatureYouControl)
        effect = Effects.CreateTokenCopyOfTarget(t) then
            Effects.If(Conditions.IsYourTurn, Effects.Scry(2))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "39"
        artist = "Winona Nelson"
        imageUri = "https://cards.scryfall.io/normal/front/9/e/9e5ba99d-d95d-4a3b-b6b6-a5e2589d538e.jpg?1783939569"
    }
}
