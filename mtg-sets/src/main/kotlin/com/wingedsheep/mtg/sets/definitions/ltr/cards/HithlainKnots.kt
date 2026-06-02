package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.dsl.LibraryPatterns

/**
 * Hithlain Knots
 * {1}{U}
 * Instant
 *
 * Tap target creature. Scry 1.
 * Draw a card.
 */
val HithlainKnots = card("Hithlain Knots") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Tap target creature. Scry 1.\nDraw a card."

    spell {
        target("target creature", Targets.Creature)
        effect = Effects.Tap(EffectTarget.ContextTarget(0)) then
            LibraryPatterns.scry(1) then
            Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "54"
        artist = "Viko Menezes"
        flavorText = "\"If you will try to run away you must be tied; but we don't wish to hurt you.\"\n—Sam"
        imageUri = "https://cards.scryfall.io/normal/front/b/b/bbcc27e7-cbe4-45c2-b157-7251a10e7ba4.jpg?1686968132"
    }
}
