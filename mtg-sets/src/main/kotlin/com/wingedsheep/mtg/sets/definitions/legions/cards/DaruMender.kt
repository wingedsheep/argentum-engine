package com.wingedsheep.mtg.sets.definitions.legions.cards

import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.RegenerateEffect

/**
 * Daru Mender
 * {W}
 * Creature — Human Cleric
 * 1/1
 * Morph {W}
 * When this creature is turned face up, regenerate target creature.
 */
val DaruMender = card("Daru Mender") {
    manaCost = "{W}"
    typeLine = "Creature — Human Cleric"
    power = 1
    toughness = 1
    oracleText = "Morph {W} (You may cast this card face down as a 2/2 creature for {3}. Turn it face up any time for its morph cost.)\nWhen this creature is turned face up, regenerate target creature."

    triggeredAbility {
        trigger = Triggers.TurnedFaceUp
        val t = target("creature", Targets.Creature)
        effect = RegenerateEffect(t)
    }

    morph = "{W}"

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "8"
        artist = "Ben Thompson"
        imageUri = "https://cards.scryfall.io/normal/front/d/e/de13fba8-3fee-4ce2-b84d-b518a99eefe0.jpg?1562939683"
    }
}
