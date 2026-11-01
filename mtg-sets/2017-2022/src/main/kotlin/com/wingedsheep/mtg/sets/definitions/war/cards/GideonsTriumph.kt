package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.predicates.StatePredicate

/**
 * Gideon's Triumph — War of the Spark #15 (canonical printing)
 * {1}{W}
 * Instant
 * Target opponent sacrifices a creature of their choice that attacked or blocked this turn. If you
 * control a Gideon planeswalker, that player sacrifices two of those creatures of their choice instead.
 *
 * "Instead" changes the count, not the event: one forced sacrifice whose count is a
 * [DynamicAmounts.conditional] read on resolution (the Chandra's Triumph shape). "Attacked or blocked
 * this turn" is the per-turn declaration markers, so a creature put onto the battlefield attacking
 * never qualifies, while one removed from combat still does.
 */
val GideonsTriumph = card("Gideon's Triumph") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Target opponent sacrifices a creature of their choice that attacked or blocked this turn. " +
        "If you control a Gideon planeswalker, that player sacrifices two of those creatures of their choice instead."

    spell {
        val opponent = target(Targets.Opponent)
        effect = Effects.Sacrifice(
            filter = GameObjectFilter.Creature.withStatePredicate(
                StatePredicate.Or(listOf(StatePredicate.AttackedThisTurn, StatePredicate.BlockedThisTurn))
            ),
            count = DynamicAmounts.conditional(
                condition = Conditions.YouControl(GameObjectFilter.Planeswalker.withSubtype("Gideon")),
                ifTrue = 2,
                ifFalse = 1
            ),
            target = opponent
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "15"
        artist = "Kieran Yanner"
        flavorText = "His triumph came not from victory over his enemies, but from the salvation of those who survived."
        imageUri = "https://cards.scryfall.io/normal/front/d/6/d6610c6a-717f-406e-a49d-b6e548880946.jpg?1783933481"
        ruling(
            "2019-05-03",
            "Gideon's Triumph can be cast after combat damage has been dealt. In this case, the target player " +
                "chooses one or two creatures that attacked or blocked this turn and have survived combat damage."
        )
        ruling("2019-05-03", "A creature that was put onto the battlefield attacking didn't attack, so it can't be sacrificed.")
        ruling(
            "2019-05-03",
            "A creature that attacked and was removed from combat (such as by Spires of Orazca) still attacked, " +
                "so it can be sacrificed."
        )
        ruling(
            "2019-05-03",
            "Gideon's Triumph targets the player, not the creature or creatures to be sacrificed. A creature with " +
                "hexproof may be sacrificed this way."
        )
        ruling("2019-05-03", "Whether you control an appropriate planeswalker is checked only as the spell resolves.")
    }
}
