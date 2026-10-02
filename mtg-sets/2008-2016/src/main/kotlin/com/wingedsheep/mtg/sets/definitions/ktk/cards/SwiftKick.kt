package com.wingedsheep.mtg.sets.definitions.ktk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate

/**
 * Swift Kick
 * {3}{R}
 * Instant
 * Target creature you control gets +1/+0 until end of turn. It fights target creature you don't control.
 */
val SwiftKick = card("Swift Kick") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Target creature you control gets +1/+0 until end of turn. It fights target creature you don't control."

    spell {
        val yourCreature = target(TargetFilter(GameObjectFilter.Creature.youControl()))
        val theirCreature = target(
            TargetFilter(
                GameObjectFilter.Creature.copy(
                    controllerPredicate = ControllerPredicate.Not(ControllerPredicate.ControlledByYou)
                )
            )
        )
        effect = Effects.ModifyStats(1, 0, yourCreature) then Effects.Fight(yourCreature, theirCreature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "122"
        artist = "Mathias Kollros"
        flavorText = "Shintan sensed the malice in his opponent, but he did not strike until the orc's muscles tensed in preparation to throw the first punch."
        imageUri = "https://cards.scryfall.io/normal/front/2/d/2dc3120c-7e04-4c4a-af16-da264593a1d1.jpg?1562784372"
    }
}
