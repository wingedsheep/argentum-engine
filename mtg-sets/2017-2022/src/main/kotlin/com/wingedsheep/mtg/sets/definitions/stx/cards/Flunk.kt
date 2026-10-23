package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.minus
import com.wingedsheep.sdk.dsl.unaryMinus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Flunk — Strixhaven: School of Mages #71 (canonical printing)
 * {1}{B} · Instant
 *
 * Target creature gets -X/-X until end of turn, where X is 7 minus the number of cards in that
 * creature's controller's hand.
 *
 * X is `7 − |hand of the target's controller|`, read at resolution ([Player.ControllerOf] reads
 * the spell's target), floored at 0 with [DynamicAmounts.nonNegative] — with seven or more cards
 * in that hand the creature gets -0/-0, never a bonus (ruling).
 */
val Flunk = card("Flunk") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Target creature gets -X/-X until end of turn, where X is 7 minus the number of cards " +
        "in that creature's controller's hand."

    spell {
        val creature = target(TargetFilter.Creature)
        val x = DynamicAmounts.nonNegative(
            7 - DynamicAmounts.count(Player.ControllerOf("target creature"), Zone.HAND)
        )
        effect = Effects.ModifyStats(power = -x, toughness = -x, target = creature)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "71"
        artist = "Randy Vargas"
        flavorText = "\"I assure you, no one fails my course more than once.\"\n—Embrose, Silverquill dean"
        imageUri = "https://cards.scryfall.io/normal/front/f/8/f8487884-e991-4feb-823b-90d9125edf19.jpg?1783927367"
        ruling("2021-04-16", "The value of X is determined as Flunk resolves.")
        ruling("2021-04-16", "If the target creature's controller has seven or more cards in hand when Flunk resolves, the creature gets -0/-0. This effect will never increase the creature's power and toughness.")
    }
}
