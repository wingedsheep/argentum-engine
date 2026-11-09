package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.entwineModal
import com.wingedsheep.sdk.dsl.entwine
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Dream's Grip
 * {U}
 * Instant
 * Choose one —
 * • Tap target permanent.
 * • Untap target permanent.
 * Entwine {1} (Choose both if you pay the entwine cost.)
 */
val DreamsGrip = card("Dream's Grip") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Choose one —\n" +
        "• Tap target permanent.\n" +
        "• Untap target permanent.\n" +
        "Entwine {1} (Choose both if you pay the entwine cost.)"

    entwine("{1}")

    spell {
        entwineModal {
            mode("Tap target permanent") {
                val permanent = target(TargetFilter.Permanent)
                effect = Effects.Tap(permanent)
            }
            mode("Untap target permanent") {
                val permanent = target(TargetFilter.Permanent)
                effect = Effects.Untap(permanent)
            }
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "34"
        artist = "Jim Nelson"
        imageUri = "https://cards.scryfall.io/normal/front/7/f/7ffaa6a2-7c86-45b4-8892-b837e05f11a6.jpg?1783944556"
    }
}
