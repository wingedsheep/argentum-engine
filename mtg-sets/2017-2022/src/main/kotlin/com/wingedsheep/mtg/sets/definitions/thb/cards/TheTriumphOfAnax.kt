package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate

/**
 * The Triumph of Anax
 * {2}{R}
 * Enchantment — Saga
 *
 * (As this Saga enters and after your draw step, add a lore counter. Sacrifice after IV.)
 * I, II, III — Until end of turn, target creature gains trample and gets +X/+0, where X is the
 *              number of lore counters on this Saga.
 * IV — Target creature you control fights up to one target creature you don't control.
 *
 * X is the Saga's lore-counter count read as each chapter resolves (ruling), then locked for the
 * turn; a Saga already gone by then counts the lore counters it last had. Chapter IV with no second
 * target fights nobody (Arni Slays the Troll's shape).
 */
val TheTriumphOfAnax = card("The Triumph of Anax") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter. Sacrifice after IV.)\n" +
        "I, II, III — Until end of turn, target creature gains trample and gets +X/+0, where X is the " +
        "number of lore counters on this Saga.\n" +
        "IV — Target creature you control fights up to one target creature you don't control. " +
        "(Each deals damage equal to its power to the other.)"

    for (chapter in 1..3) {
        sagaChapter(chapter) {
            val creature = target(TargetFilter.Creature)
            effect = Effects.GrantKeyword(Keyword.TRAMPLE, creature) then
                Effects.ModifyStats(
                    power = DynamicAmounts.countersOnSelf(CounterType.LORE),
                    toughness = DynamicAmounts.fixed(0),
                    target = creature,
                )
        }
    }

    sagaChapter(4) {
        val mine = target(TargetFilter.CreatureYouControl)
        val theirs = target(
            TargetFilter(
                GameObjectFilter.Creature.copy(
                    controllerPredicate = ControllerPredicate.Not(ControllerPredicate.ControlledByYou)
                )
            ),
            optional = true
        )
        effect = Effects.Fight(mine, theirs)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "160"
        artist = "Chris Seaman"
        imageUri = "https://cards.scryfall.io/normal/front/4/9/49b5b348-1415-4062-a2a7-017694424c05.jpg?1783931543"
        ruling(
            "2020-01-24",
            "The value of X for the first three chapters is determined only as each chapter ability " +
                "resolves. Once that happens, the value of X won't change later in the turn even if the " +
                "number of lore counters on The Triumph of Anax changes."
        )
        ruling(
            "2020-01-24",
            "If either target is an illegal target as the final chapter ability resolves, no creature " +
                "will deal or be dealt damage."
        )
    }
}
