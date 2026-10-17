package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate

/**
 * Prizefight
 * {1}{G}
 * Instant
 *
 * Target creature you control fights target creature you don't control.
 * Create a Treasure token.
 *
 * "You don't control" is `Not(ControlledByYou)` rather than "an opponent controls" — the same
 * spelling as Go for Blood.
 */
val Prizefight = card("Prizefight") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Target creature you control fights target creature you don't control. " +
        "(Each deals damage equal to its power to the other.)\n" +
        "Create a Treasure token. (It's an artifact with \"{T}, Sacrifice this token: Add one mana of any color.\")"

    spell {
        val yourCreature = target(TargetFilter(GameObjectFilter.Creature.youControl()))
        val theirCreature = target(
            TargetFilter(
                GameObjectFilter.Creature.copy(
                    controllerPredicate = ControllerPredicate.Not(ControllerPredicate.ControlledByYou)
                )
            )
        )
        effect = Effects.Fight(yourCreature, theirCreature) then Effects.CreateTreasure()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "154"
        artist = "Deruchenko Alexander"
        flavorText = "Underweight, underpaid, underestimated."
        imageUri = "https://cards.scryfall.io/normal/front/5/4/544c810b-5f90-4535-aa76-14f8c6b9428a.jpg?1783923100"
    }
}
