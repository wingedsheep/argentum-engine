package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.Exists
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Flames of the Raze-Boar
 * {5}{R}
 * Instant
 * Flames of the Raze-Boar deals 4 damage to target creature an opponent controls. Then Flames of
 * the Raze-Boar deals 2 damage to each other creature that player controls if you control a
 * creature with power 4 or greater.
 *
 * Fear, Fire, Foes!'s group shape: "that player" is the target creature's controller
 * (`targetPlayerControls(TargetController)`) and `otherThanTarget()` leaves the target out. The
 * ferocious check is made once, as the spell resolves, after the 4 damage (Temur Battle Rage's
 * shape); the target is still on the battlefield then, since SBAs wait for resolution to finish.
 */
val FlamesOfTheRazeBoar = card("Flames of the Raze-Boar") {
    manaCost = "{5}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Flames of the Raze-Boar deals 4 damage to target creature an opponent controls. " +
        "Then Flames of the Raze-Boar deals 2 damage to each other creature that player controls " +
        "if you control a creature with power 4 or greater."

    spell {
        val creature = target(TargetFilter.CreatureOpponentControls)
        effect = Effects.DealDamage(4, creature) then
            Effects.If(
                condition = Exists(Player.You, Zone.BATTLEFIELD, GameObjectFilter.Creature.powerAtLeast(4)),
                then = Patterns.Group.dealDamageToAll(
                    2,
                    GroupFilter(
                        GameObjectFilter.Creature.targetPlayerControls(EffectTarget.TargetController)
                    ).otherThanTarget()
                )
            )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "101"
        artist = "Jason A. Engle"
        flavorText = "\"Fire will cure a multitude of ills.\""
        imageUri = "https://cards.scryfall.io/normal/front/1/6/16957271-12bb-4031-b476-f7678b753ae3.jpg?1783933682"
        ruling(
            "2019-01-25",
            "If the target is dealt lethal damage this way, it will still be on the battlefield while Flames of the Raze-Boar deals damage to other creatures, if appropriate. Creatures dealt lethal damage won't die until after Flames of the Raze-Boar is entirely finished resolving.",
        )
        ruling(
            "2019-01-25",
            "If the chosen target becomes an illegal target for Flames of the Raze-Boar, the spell doesn't resolve. No creatures are dealt damage.",
        )
    }
}
