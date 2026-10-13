package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate

/**
 * Tail Swipe
 * {G}
 * Instant
 * Choose target creature you control and target creature you don't control. If you cast this spell
 * during your main phase, the creature you control gets +1/+1 until end of turn. Then those creatures
 * fight each other. (Each deals damage equal to its power to the other.)
 *
 * "If you cast this spell during your main phase" is checked at resolution with
 * [Conditions.IsYourMainPhase]: a spell can't leave the phase it was cast in before it resolves
 * (a phase only ends with an empty stack), so the resolution-time phase is the cast-time phase.
 * A copy isn't cast, so strictly it shouldn't get the bonus; this reading grants it anyway.
 */
val TailSwipe = card("Tail Swipe") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Choose target creature you control and target creature you don't control. " +
        "If you cast this spell during your main phase, the creature you control gets +1/+1 until " +
        "end of turn. Then those creatures fight each other. (Each deals damage equal to its power " +
        "to the other.)"

    spell {
        val mine = target(TargetFilter.CreatureYouControl)
        val theirs = target(
            TargetFilter(
                GameObjectFilter.Creature.copy(
                    controllerPredicate = ControllerPredicate.Not(ControllerPredicate.ControlledByYou)
                )
            )
        )

        effect = Effects.If(
            condition = Conditions.IsYourMainPhase,
            then = Effects.ModifyStats(1, 1, mine),
        ) then Effects.Fight(mine, theirs)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "182"
        artist = "Ângelo Bortolini"
        imageUri = "https://cards.scryfall.io/normal/front/9/5/95a39b26-8c83-40ea-b492-036251366d73.jpg?1783921295"
    }
}
