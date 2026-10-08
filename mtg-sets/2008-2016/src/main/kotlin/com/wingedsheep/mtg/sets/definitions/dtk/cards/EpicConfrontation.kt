package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate

/**
 * Epic Confrontation — Dragons of Tarkir #185
 * {1}{G}
 * Sorcery
 * Target creature you control gets +1/+2 until end of turn. It fights target creature you don't
 * control. (Each deals damage equal to its power to the other.)
 *
 * Same shape as Ruthless Predation: the pump resolves before the fight, so the bonus counts toward
 * both the damage dealt and the damage survived. "You don't control" is `Not(ControlledByYou)`.
 */
val EpicConfrontation = card("Epic Confrontation") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Target creature you control gets +1/+2 until end of turn. It fights target creature you don't control. (Each deals damage equal to its power to the other.)"

    spell {
        val mine = target(TargetFilter.CreatureYouControl)
        val theirs = target(
            TargetFilter(
                GameObjectFilter.Creature.copy(
                    controllerPredicate = ControllerPredicate.Not(ControllerPredicate.ControlledByYou)
                )
            )
        )
        effect = Effects.ModifyStats(1, 2, mine) then Effects.Fight(mine, theirs)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "185"
        artist = "Wayne Reynolds"
        flavorText = "No matter the timeline, some legends will endure."
        imageUri = "https://cards.scryfall.io/normal/front/f/5/f587436e-51ff-4c9c-a6ce-e2768844a71a.jpg"
    }
}
