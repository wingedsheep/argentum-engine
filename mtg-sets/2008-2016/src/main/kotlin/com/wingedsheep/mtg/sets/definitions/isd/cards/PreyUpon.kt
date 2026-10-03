package com.wingedsheep.mtg.sets.definitions.isd.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate

/**
 * Prey Upon
 * {G}
 * Sorcery
 * Target creature you control fights target creature you don't control. (Each deals damage equal to its power to the other.)
 */
val PreyUpon = card("Prey Upon") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
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
        artist = "Dave Kendall"
        flavorText = "\"You don't find many old werewolf hunters.\"\n—Paulin, trapper of Somberwald"
        imageUri = "https://cards.scryfall.io/normal/front/b/7/b7b3eaf0-4207-4bac-923d-29f348c95a35.jpg"
    }
}
