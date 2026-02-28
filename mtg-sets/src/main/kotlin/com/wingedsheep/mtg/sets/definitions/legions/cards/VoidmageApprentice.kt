package com.wingedsheep.mtg.sets.definitions.legions.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Voidmage Apprentice
 * {1}{U}
 * Creature — Human Wizard
 * 1/1
 * Morph {2}{U}{U} (You may cast this card face down as a 2/2 creature for {3}. Turn it face up any time for its morph cost.)
 * When this creature is turned face up, counter target spell.
 */
val VoidmageApprentice = card("Voidmage Apprentice") {
    manaCost = "{1}{U}"
    typeLine = "Creature — Human Wizard"
    power = 1
    toughness = 1
    oracleText = "Morph {2}{U}{U} (You may cast this card face down as a 2/2 creature for {3}. Turn it face up any time for its morph cost.)\nWhen this creature is turned face up, counter target spell."

    triggeredAbility {
        trigger = Triggers.TurnedFaceUp
        target = Targets.Spell
        effect = Effects.CounterSpell()
    }

    morph = "{2}{U}{U}"

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "54"
        artist = "Jim Nelson"
        imageUri = "https://cards.scryfall.io/normal/front/5/5/55924a25-e749-48f6-8ef1-1fa8376f96b1.jpg?1562912415"
        ruling("2013-04-15", "If a spell with split second is on the stack, you can still respond by turning this creature face up and targeting that spell with the trigger.")
    }
}
