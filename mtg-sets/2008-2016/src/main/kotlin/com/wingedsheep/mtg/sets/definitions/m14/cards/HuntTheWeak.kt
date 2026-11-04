package com.wingedsheep.mtg.sets.definitions.m14.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate

/**
 * Hunt the Weak
 * {3}{G}
 * Sorcery
 * Put a +1/+1 counter on target creature you control. Then that creature fights target creature you
 * don't control.
 *
 * The counter lands first, so the fight uses the boosted power. If only the opposing target is
 * illegal, the counter still goes on and the fight does nothing.
 */
val HuntTheWeak = card("Hunt the Weak") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Put a +1/+1 counter on target creature you control. Then that creature fights target " +
        "creature you don't control. (Each deals damage equal to its power to the other.)"

    spell {
        val mine = target(TargetFilter.CreatureYouControl)
        // "you don't control", not "an opponent controls" — a teammate's creature is a legal target.
        val theirs = target(
            TargetFilter(
                GameObjectFilter.Creature.copy(
                    controllerPredicate = ControllerPredicate.Not(ControllerPredicate.ControlledByYou)
                )
            )
        )
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, mine) then
            Effects.Fight(mine, theirs)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "179"
        artist = "Raoul Vitale"
        flavorText = "He who hesitates is lunch."
        imageUri = "https://cards.scryfall.io/normal/front/8/f/8f7a6df7-acfc-4047-b119-505f4277225c.jpg?1783939904"
        ruling("2018-01-19", "You can't cast Hunt the Weak unless you choose both a creature you control and a creature you don't control as targets.")
        ruling("2018-01-19", "If either target is an illegal target as Hunt the Weak resolves, neither creature will deal or be dealt damage.")
        ruling("2018-01-19", "If the creature you control is an illegal target as Hunt the Weak tries to resolve, you won't put a +1/+1 counter on it. If that creature is a legal target but the other creature isn't, you'll still put the counter on the creature you control.")
    }
}
