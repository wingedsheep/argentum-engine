package com.wingedsheep.mtg.sets.definitions.bok.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Takenuma Bleeder
 * {2}{B}
 * Creature — Ogre Shaman
 * 3/3
 * Whenever this creature attacks or blocks, you lose 1 life if you don't control a Demon.
 *
 * "Attacks or blocks" is two triggered abilities sharing one effect (the Merfolk Skyscout shape).
 * The trailing "if" is part of the effect, not an intervening-if, so the Demon check happens on
 * resolution (the Gutwrencher Oni shape).
 */
val TakenumaBleeder = card("Takenuma Bleeder") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Ogre Shaman"
    power = 3
    toughness = 3
    oracleText = "Whenever this creature attacks or blocks, you lose 1 life if you don't control a Demon."

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.If(
            Conditions.Not(Conditions.ControlPermanentOfType(Subtype("Demon"))),
            Effects.LoseLife(1, EffectTarget.Controller)
        )
    }

    triggeredAbility {
        trigger = Triggers.self.blocks()
        effect = Effects.If(
            Conditions.Not(Conditions.ControlPermanentOfType(Subtype("Demon"))),
            Effects.LoseLife(1, EffectTarget.Controller)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "86"
        artist = "Kev Walker"
        flavorText = "\"I prefer to weave my magic through oni blood, but yours will do in a pinch.\""
        imageUri = "https://cards.scryfall.io/normal/front/b/f/bfa3d318-83ff-41db-b33a-e4f5b7cd7a77.jpg?1783944195"
    }
}
