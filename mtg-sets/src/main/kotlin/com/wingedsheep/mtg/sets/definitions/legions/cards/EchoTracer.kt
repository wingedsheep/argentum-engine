package com.wingedsheep.mtg.sets.definitions.legions.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Echo Tracer
 * {2}{U}
 * Creature — Human Wizard
 * 2/2
 * Morph {2}{U} (You may cast this card face down as a 2/2 creature for {3}. Turn it face up any time for its morph cost.)
 * When this creature is turned face up, return target creature to its owner's hand.
 */
val EchoTracer = card("Echo Tracer") {
    manaCost = "{2}{U}"
    typeLine = "Creature — Human Wizard"
    power = 2
    toughness = 2
    oracleText = "Morph {2}{U} (You may cast this card face down as a 2/2 creature for {3}. Turn it face up any time for its morph cost.)\nWhen this creature is turned face up, return target creature to its owner's hand."

    triggeredAbility {
        trigger = Triggers.TurnedFaceUp
        val t = target("creature", Targets.Creature)
        effect = Effects.ReturnToHand(t)
    }

    morph = "{2}{U}"

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "37"
        artist = "Scott M. Fischer"
        imageUri = "https://cards.scryfall.io/normal/front/6/3/63390760-35a7-4b4c-8c68-5c84f90d0c58.jpg?1562915084"
    }
}
