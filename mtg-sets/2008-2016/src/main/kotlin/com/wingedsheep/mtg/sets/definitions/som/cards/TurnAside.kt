package com.wingedsheep.mtg.sets.definitions.som.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate

/**
 * Turn Aside — Scars of Mirrodin #49
 * {U} · Instant
 *
 * Counter target spell that targets a permanent you control.
 *
 * Keep Safe without the draw: a spell on the stack at least one of whose chosen targets is a
 * permanent you control ([CardPredicate.TargetsMatching]).
 */
val TurnAside = card("Turn Aside") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Counter target spell that targets a permanent you control."

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
        effect = Effects.CounterSpell()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "49"
        artist = "Shelly Wan"
        flavorText = "\"It's not my job to ask our leaders about their experiments at the Knowledge Pool. It's my job to make sure they can continue performing them.\""
        imageUri = "https://cards.scryfall.io/normal/front/5/6/56226f57-6ff0-430e-aba6-6b3dd51f8d3c.jpg?1783941734"
        ruling("2016-07-13", "Turn Aside can target a spell that has multiple targets, as long as at least one of those targets is a permanent you control.")
    }
}
