package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Reject — Strixhaven: School of Mages #50 (canonical printing)
 * {1}{U} · Instant
 *
 * Counter target creature or planeswalker spell unless its controller pays {3}. If that spell is
 * countered this way, exile it instead of putting it into its owner's graveyard.
 *
 * No More Lies' [Effects.CounterUnlessDynamicPays] with `exileOnCounter`, narrowed to creature or
 * planeswalker spells on the stack.
 */
val Reject = card("Reject") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Counter target creature or planeswalker spell unless its controller pays {3}. If " +
        "that spell is countered this way, exile it instead of putting it into its owner's graveyard."

    spell {
        target(TargetFilter(GameObjectFilter.Creature or GameObjectFilter.Planeswalker, zone = Zone.STACK))
        effect = Effects.CounterUnlessDynamicPays(DynamicAmounts.fixed(3), exileOnCounter = true)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "50"
        artist = "Zoltan Boros"
        flavorText = "\"Grux, for the last time, you have to do more than simply shout 'Apply!' at the " +
            "administrators.\"\n—Pavil, admissions officer"
        imageUri = "https://cards.scryfall.io/normal/front/d/7/d77f0731-fb40-4dc2-8530-afcb5ce1f27f.jpg?1783927375"
    }
}
