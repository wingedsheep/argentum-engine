package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * You Come to a River
 * {1}{U}
 * Instant
 *
 * Choose one —
 * • Fight the Current — Return target nonland permanent to its owner's hand.
 * • Find a Crossing — Target creature gets +1/+0 until end of turn and can't be blocked this turn.
 *
 * A plain choose-one `modal(chooseCount = 1)` with one target per mode (the Crosis's Charm shape):
 * a bounce over [TargetFilter.NonlandPermanent], and a +1/+0 pump followed by the
 * [AbilityFlag.CANT_BE_BLOCKED] grant on the same creature, both lasting until end of turn.
 */
val YouComeToARiver = card("You Come to a River") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Choose one —\n" +
        "• Fight the Current — Return target nonland permanent to its owner's hand.\n" +
        "• Find a Crossing — Target creature gets +1/+0 until end of turn and can't be blocked this turn."

    spell {
        modal(chooseCount = 1) {
            mode("Fight the Current — Return target nonland permanent to its owner's hand.") {
                val permanent = target(TargetFilter.NonlandPermanent)
                effect = Effects.ReturnToHand(permanent)
            }
            mode("Find a Crossing — Target creature gets +1/+0 and can't be blocked this turn.") {
                val creature = target(TargetFilter.Creature)
                effect = Effects.ModifyStats(1, 0, creature) then
                    Effects.GrantKeyword(AbilityFlag.CANT_BE_BLOCKED, creature)
            }
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "83"
        artist = "Viko Menezes"
        imageUri = "https://cards.scryfall.io/normal/front/e/b/ebf9bab7-98a0-4394-90c0-e7c52e14eb37.jpg?1783926504"
    }
}
