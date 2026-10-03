package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate

/**
 * Pounce
 * {1}{G}
 * Instant
 * Target creature you control fights target creature you don't control. (Each deals damage equal to its power to the other.)
 */
val Pounce = card("Pounce") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Target creature you control fights target creature you don't control. (Each deals damage equal to its power to the other.)"

    spell {
        val mine = target(TargetFilter.CreatureYouControl)
        val theirs = target(
            TargetFilter(
                GameObjectFilter.Creature.copy(
                    controllerPredicate = ControllerPredicate.Not(ControllerPredicate.ControlledByYou)
                )
            )
        )
        effect = Effects.Fight(mine, theirs)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "200"
        artist = "Lucas Graciano"
        flavorText = "The drive to hunt and feed is raw instinct for dinosaurs. The trick is simply to channel it in the right direction."
        imageUri = "https://cards.scryfall.io/normal/front/2/b/2b96cba6-33d7-4e1d-88f7-da3da681540d.jpg"
    }
}
