package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate

/**
 * Keep Safe — Ikoria: Lair of Behemoths #56
 * {1}{U} · Instant
 *
 * Counter target spell that targets a permanent you control.
 * Draw a card.
 *
 * Same target shape as Avoid Fate without the instant/Aura restriction: a spell on the stack at
 * least one of whose chosen targets is a permanent you control ([CardPredicate.TargetsMatching]).
 * The draw is unconditional, so an uncounterable target still draws a card.
 */
val KeepSafe = card("Keep Safe") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Counter target spell that targets a permanent you control.\nDraw a card."

    spell {
        target(
            TargetFilter(
                baseFilter = GameObjectFilter(
                    cardPredicates = listOf(
                        CardPredicate.TargetsMatching(GameObjectFilter.Permanent.youControl())
                    )
                ),
                zone = Zone.STACK
            )
        )
        effect = Effects.CounterSpell() then Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "56"
        artist = "Ekaterina Burmak"
        flavorText = "\"Coppercoats think they understand duty and loyalty, but they can't begin to fathom the depths of my bond.\""
        imageUri = "https://cards.scryfall.io/normal/front/f/e/febfa682-76ae-4979-a40c-c1eae1121f3c.jpg?1783931075"
        ruling("2020-04-17", "A spell that can't be countered is a legal target for Keep Safe. That spell won't be countered when Keep Safe resolves, but you'll still draw a card.")
        ruling("2020-04-17", "Keep Safe can target a spell that has multiple targets, as long as at least one of those targets is a permanent you control.")
        ruling("2020-04-17", "If the permanent you control targeted by the target spell leaves the battlefield, that spell is no longer a legal target for Keep Safe. On the other hand, if that target permanent becomes an illegal target for the target spell but remains on the battlefield under your control, the target spell is still a legal target for Keep Safe.")
    }
}
