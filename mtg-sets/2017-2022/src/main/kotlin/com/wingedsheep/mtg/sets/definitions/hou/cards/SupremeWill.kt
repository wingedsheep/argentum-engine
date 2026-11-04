package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Supreme Will — Hour of Devastation #102 (canonical printing)
 * {2}{U} · Instant
 *
 * Choose one —
 * • Counter target spell unless its controller pays {3}.
 * • Look at the top four cards of your library. Put one of them into your hand and the rest on
 *   the bottom of your library in any order.
 */
val SupremeWill = card("Supreme Will") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Choose one —\n" +
        "• Counter target spell unless its controller pays {3}.\n" +
        "• Look at the top four cards of your library. Put one of them into your hand and the rest on the bottom of your library in any order."

    spell {
        modal(chooseCount = 1) {
            mode("Counter target spell unless its controller pays {3}") {
                target(TargetFilter.SpellOnStack)
                effect = Effects.CounterUnlessPays("{3}")
            }
            mode("Look at the top four cards of your library. Put one of them into your hand and the rest on the bottom of your library in any order") {
                effect = Patterns.Library.lookAtTopAndKeep(
                    count = 4,
                    keepCount = 1,
                    keepDestination = CardDestination.ToZone(Zone.HAND),
                    restDestination = CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Bottom),
                    restOrder = CardOrder.ControllerChooses
                )
            }
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "102"
        artist = "Alayna Danner"
        imageUri = "https://cards.scryfall.io/normal/front/f/c/fcf7fd8a-adc3-4099-8e30-08375f263446.jpg?1783928848"
    }
}
