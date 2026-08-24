package com.wingedsheep.mtg.sets.definitions.fem.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.TargetOpponent
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Dwarven Catapult
 * {X}{R}
 * Instant
 * Dwarven Catapult deals X damage divided evenly, rounded down, among all creatures target
 * opponent controls.
 *
 * "Divided evenly, rounded down" is one division done on resolution, not a free assignment: each
 * creature the target opponent controls takes floor(X / N), where N is how many such creatures
 * there are. With more creatures than X, every creature takes 0 — no damage is dealt at all.
 *
 * Composed rather than given its own effect type: the group deals a per-creature amount, and that
 * amount is [DynamicAmount.Divide] of X over the same population, rounding down. Nothing leaves
 * the battlefield mid-resolution (state-based actions wait until the spell finishes, CR 704.3),
 * so the divisor is stable across the group.
 */
val DwarvenCatapult = card("Dwarven Catapult") {
    manaCost = "{X}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Dwarven Catapult deals X damage divided evenly, rounded down, among all " +
        "creatures target opponent controls."

    spell {
        target = TargetOpponent()
        effect = Patterns.Group.dealDamageToAll(
            amount = DynamicAmount.Divide(
                numerator = DynamicAmount.XValue,
                denominator = DynamicAmount.AggregateBattlefield(
                    Player.TargetOpponent,
                    GameObjectFilter.Creature
                ),
                roundUp = false
            ),
            filter = GroupFilter(
                GameObjectFilter.Creature.copy(
                    controllerPredicate = ControllerPredicate.ControlledByTargetOpponent
                )
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "51"
        artist = "Jeff A. Menges"
        flavorText = "\"Often greatly outnumbered in battle, Dwarves relied on catapults as one means of damaging a large army.\"\n—*Sarpadian Empires, vol. IV*"
        imageUri = "https://cards.scryfall.io/normal/front/8/c/8c1c6932-638a-4df7-bf9b-8d921f7484d9.jpg?1783947896"
    }
}
