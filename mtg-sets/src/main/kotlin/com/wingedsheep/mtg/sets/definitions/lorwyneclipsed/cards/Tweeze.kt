package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.MayEffect

/**
 * Tweeze
 * {2}{R}
 * Instant
 *
 * Tweeze deals 3 damage to any target. You may discard a card. If you do, draw a card.
 */
val Tweeze = card("Tweeze") {
    manaCost = "{2}{R}"
    typeLine = "Instant"
    oracleText = "Tweeze deals 3 damage to any target. You may discard a card. If you do, draw a card."

    spell {
        val damageTarget = target("target to deal 3 damage", Targets.Any)
        effect = CompositeEffect(listOf(
            Effects.DealDamage(3, damageTarget),
            MayEffect(
                effect = CompositeEffect(listOf(
                    EffectPatterns.discardCards(1),
                    Effects.DrawCards(1)
                )),
                description_override = "You may discard a card. If you do, draw a card."
            )
        ))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "162"
        artist = "Scott Gustafson"
        flavorText = "\"Oh, come on, don't be stingy! You've got so many!\""
        imageUri = "https://cards.scryfall.io/normal/front/3/c/3ceab0e6-1bb8-487d-ab4b-2da8457b970a.jpg?1767873738"
        ruling(
            "2025-11-17",
            "If the target is illegal as Tweeze tries to resolve, it won't resolve and none of its effects will happen. You won't have the opportunity to discard a card to draw a card."
        )
    }
}
