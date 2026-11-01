package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate

/**
 * Savage Smash — Ravnica Allegiance #203
 * {1}{R}{G} · Sorcery
 */
val SavageSmash = card("Savage Smash") {
    manaCost = "{1}{R}{G}"
    colorIdentity = "GR"
    typeLine = "Sorcery"
    oracleText = "Target creature you control gets +2/+2 until end of turn. It fights target creature " +
        "you don't control. (Each deals damage equal to its power to the other.)"

    spell {
        val mine = target(TargetFilter.CreatureYouControl)
        val theirs = target(
            TargetFilter(
                GameObjectFilter.Creature.copy(
                    controllerPredicate = ControllerPredicate.Not(ControllerPredicate.ControlledByYou)
                )
            )
        )
        effect = Effects.ModifyStats(2, 2, mine) then Effects.Fight(mine, theirs)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "203"
        artist = "Zoltan Boros"
        flavorText = "A Gruul berserker is never unarmed."
        imageUri = "https://cards.scryfall.io/normal/front/4/c/4ca942d7-a3a3-429f-a159-fc2363d9bca6.jpg?1783933639"
    }
}
