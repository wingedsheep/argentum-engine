package com.wingedsheep.mtg.sets.definitions.j22.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Soul Read
 * {3}{U}
 * Instant
 * Choose one —
 * • Counter target spell unless its controller pays {4}.
 * • Draw two cards.
 *
 * A plain choose-one modal: the tax counter is [Effects.CounterUnlessPays] over the mode's
 * spell target (as on Temur Charm), and the draw mode targets nothing, so it can be cast with
 * an empty stack.
 */
val SoulRead = card("Soul Read") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Choose one —\n• Counter target spell unless its controller pays {4}.\n• Draw two cards."

    spell {
        modal(chooseCount = 1) {
            mode("Counter target spell unless its controller pays {4}") {
                target(TargetFilter.SpellOnStack)
                effect = Effects.CounterUnlessPays("{4}")
            }
            mode("Draw two cards") {
                effect = Effects.DrawCards(2)
            }
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "17"
        artist = "Drew Tucker"
        flavorText = "It's easy to be two steps ahead when you can see their every thought."
        imageUri = "https://cards.scryfall.io/normal/front/2/c/2cebbfa1-e709-45bb-9b86-c6b09e7866b2.jpg?1783919192"
    }
}
