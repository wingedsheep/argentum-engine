package com.wingedsheep.mtg.sets.definitions.m13.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Public Execution
 * {5}{B}
 * Instant
 * Destroy target creature an opponent controls. Each other creature that player controls gets
 * -2/-0 until end of turn.
 *
 * Flames of the Raze-Boar's group shape: "that player" is the target's controller
 * (`targetPlayerControls(TargetController)`, read off last-known information once the target has
 * been destroyed) and `otherThanTarget()` leaves the target out even when it survives by
 * regeneration or indestructible. The group is fixed as the spell resolves.
 */
val PublicExecution = card("Public Execution") {
    manaCost = "{5}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Destroy target creature an opponent controls. Each other creature that player controls " +
        "gets -2/-0 until end of turn."

    spell {
        val creature = target(TargetFilter.CreatureOpponentControls)
        effect = Effects.Destroy(creature) then
            Patterns.Group.modifyStatsForAll(
                -2,
                0,
                GroupFilter(
                    GameObjectFilter.Creature.targetPlayerControls(EffectTarget.TargetController)
                ).otherThanTarget()
            )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "105"
        artist = "Anthony Palumbo"
        flavorText = "Though the executioner did not speak, the villagers got the message."
        imageUri = "https://cards.scryfall.io/normal/front/4/8/48188942-d0ba-4503-bd75-c7a5329bb7c8.jpg?1783940491"
        ruling(
            "2013-07-01",
            "If the target creature regenerates or has indestructible, each other creature controlled by that creature's controller will still get -2/-0.",
        )
        ruling(
            "2012-07-01",
            "Only creatures controlled by the player when Public Execution resolves will get -2/-0. Creatures that enter under that player's control or that the player gains control of later in the turn are unaffected.",
        )
        ruling(
            "2012-07-01",
            "Public Execution targets only the creature it destroys. If that creature is an illegal target when Public Execution tries to resolve, it won't resolve and none of its effects will happen. No creature will get -2/-0.",
        )
    }
}
