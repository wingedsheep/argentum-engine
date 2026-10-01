package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Eye of Malcator
 * {2}{U}
 * Artifact
 *
 * When this artifact enters, scry 2.
 * Whenever another artifact you control enters, this artifact becomes a 4/4 Phyrexian Eye
 * artifact creature until end of turn.
 *
 * It is already an artifact, so the animate only adds CREATURE (Layer 4), sets the Phyrexian Eye
 * creature subtypes (it has none of its own to lose), and sets base P/T 4/4 (Layer 7b) until end
 * of turn.
 */
val EyeOfMalcator = card("Eye of Malcator") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Artifact"
    oracleText = "When this artifact enters, scry 2. (Look at the top two cards of your library, then put any number of them on the bottom and the rest on top in any order.)\n" +
        "Whenever another artifact you control enters, this artifact becomes a 4/4 Phyrexian Eye artifact creature until end of turn."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Scry(2)
    }

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Artifact.youControl()).enters()
        effect = Effects.BecomeCreature(
            target = EffectTarget.Self,
            power = 4,
            toughness = 4,
            creatureTypes = setOf("Phyrexian", "Eye"),
            duration = Duration.EndOfTurn,
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "50"
        artist = "Jonas De Ro"
        imageUri = "https://cards.scryfall.io/normal/front/7/c/7cc4246b-ff59-44b2-acff-c0bb6e4cd6ac.jpg?1783918065"
    }
}
