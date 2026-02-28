package com.wingedsheep.mtg.sets.definitions.scourge.cards

import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.BecomeCreatureTypeEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Proteus Machine
 * {3}
 * Artifact Creature — Shapeshifter
 * 2/2
 * Morph {0}
 * When this creature is turned face up, it becomes the creature type of your choice.
 */
val ProteusMachine = card("Proteus Machine") {
    manaCost = "{3}"
    typeLine = "Artifact Creature — Shapeshifter"
    power = 2
    toughness = 2
    oracleText = "Morph {0} (You may cast this card face down as a 2/2 creature for {3}. Turn it face up any time for its morph cost.)\nWhen this creature is turned face up, it becomes the creature type of your choice."

    morph = "{0}"

    triggeredAbility {
        trigger = Triggers.TurnedFaceUp
        effect = BecomeCreatureTypeEffect(
            target = EffectTarget.Self,
            duration = Duration.Permanent
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "141"
        artist = "Greg Staples"
        imageUri = "https://cards.scryfall.io/normal/front/d/1/d1c8cff1-b289-41a4-9fa3-cc5e7ba70802.jpg?1562534821"
    }
}
