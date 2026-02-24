package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.DrawCardsEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetOpponent

/**
 * Wheel and Deal
 * {3}{U}
 * Instant
 * Any number of target opponents each discard their hands, then draw seven cards. Draw a card.
 */
val WheelAndDeal = card("Wheel and Deal") {
    manaCost = "{3}{U}"
    typeLine = "Instant"
    oracleText = "Any number of target opponents each discard their hands, then draw seven cards. Draw a card."

    spell {
        val t = target("target", TargetOpponent())
        effect = CompositeEffect(
            listOf(
                Effects.DiscardHand(t),
                DrawCardsEffect(7, target = t),
                DrawCardsEffect(1, target = EffectTarget.Controller)
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "121"
        artist = "Alan Pollack"
        flavorText = "\"Give a man a fish, you feed him for a day. Give a man someone else's fish, you make a friend for life.\""
        imageUri = "https://cards.scryfall.io/normal/front/6/1/61f50a1a-f3d0-4fcf-bd32-0e173b0d3247.jpg?1562918070"
    }
}
