package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Trostani's Judgment
 * {5}{W}
 * Instant
 *
 * Exile target creature, then populate. (Create a token that's a copy of a creature token you
 * control.)
 *
 * [Effects.Exile] on the target, then populate as on Coursers' Accord:
 * [Effects.CreateTokenCopyOfChosenPermanent] over creature tokens you control, chosen on
 * resolution. If the exiled creature was your own token, it is already gone when populate
 * chooses, so it can't be copied.
 *
 * Canonical printing: Return to Ravnica, the card's earliest real printing.
 */
val TrostanisJudgment = card("Trostani's Judgment") {
    manaCost = "{5}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Exile target creature, then populate. (Create a token that's a copy of a creature " +
        "token you control.)"

    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.Exile(t) then
            Effects.CreateTokenCopyOfChosenPermanent(GameObjectFilter.Creature.token())
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "28"
        artist = "Christopher Moeller"
        flavorText = "\"Some cannot be saved as they are, but all can serve the Worldsoul.\"\n—Trostani"
        imageUri = "https://cards.scryfall.io/normal/front/d/7/d707bdb1-1f8c-4cc0-be01-496f3f03b878.jpg?1783940371"
    }
}
