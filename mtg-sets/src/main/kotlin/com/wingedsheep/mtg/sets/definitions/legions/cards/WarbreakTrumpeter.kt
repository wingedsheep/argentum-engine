package com.wingedsheep.mtg.sets.definitions.legions.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Warbreak Trumpeter
 * {R}
 * Creature — Goblin
 * 1/1
 * Morph {X}{X}{R} (You may cast this card face down as a 2/2 creature for {3}. Turn it face up any time for its morph cost.)
 * When Warbreak Trumpeter is turned face up, create X 1/1 red Goblin creature tokens.
 */
val WarbreakTrumpeter = card("Warbreak Trumpeter") {
    manaCost = "{R}"
    typeLine = "Creature — Goblin"
    oracleText = "Morph {X}{X}{R} (You may cast this card face down as a 2/2 creature for {3}. Turn it face up any time for its morph cost.)\nWhen Warbreak Trumpeter is turned face up, create X 1/1 red Goblin creature tokens."
    power = 1
    toughness = 1

    triggeredAbility {
        trigger = Triggers.TurnedFaceUp
        effect = CreateTokenEffect(
            count = DynamicAmount.XValue,
            power = 1,
            toughness = 1,
            colors = setOf(Color.RED),
            creatureTypes = setOf("Goblin"),
            imageUri = "https://cards.scryfall.io/normal/front/e/d/ed418a8b-f158-492d-a323-6265b3175292.jpg?1562640121"
        )
    }

    morph = "{X}{X}{R}"

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "116"
        artist = "Dany Orizio"
        imageUri = "https://cards.scryfall.io/normal/front/f/c/fc942957-1067-428c-8ee1-01f9e260efe1.jpg?1562945961"
    }
}
