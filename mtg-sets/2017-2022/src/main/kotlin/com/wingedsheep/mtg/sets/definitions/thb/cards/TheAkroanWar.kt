package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.MustAttackDefender
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * The Akroan War
 * {3}{R}
 * Enchantment — Saga
 *
 * (As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)
 * I — Gain control of target creature for as long as this Saga remains on the battlefield.
 * II — Until your next turn, creatures your opponents control attack each combat if able.
 * III — Each tapped creature deals damage to itself equal to its power.
 *
 * Chapter I is the Sower of Temptation steal keyed to the Saga
 * ([Duration.WhileSourceOnBattlefield]): if the Saga is gone before it resolves, nothing is
 * stolen, and the creature goes back when the Saga is sacrificed after chapter III resolves.
 * Chapter II is Kardur's durational group must-attack without the defender restriction.
 * Chapter III walks every tapped creature, each the source of its own damage (Kiku's shape).
 */
val TheAkroanWar = card("The Akroan War") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)\n" +
        "I — Gain control of target creature for as long as this Saga remains on the battlefield.\n" +
        "II — Until your next turn, creatures your opponents control attack each combat if able.\n" +
        "III — Each tapped creature deals damage to itself equal to its power."

    sagaChapter(1) {
        val creature = target(TargetFilter.Creature)
        effect = Effects.GainControl(creature, Duration.WhileSourceOnBattlefield("The Akroan War"))
    }

    sagaChapter(2) {
        effect = Effects.MustAttackGroup(
            GroupFilter.AllCreaturesOpponentsControl,
            MustAttackDefender.Any,
            Duration.UntilYourNextTurn,
        )
    }

    sagaChapter(3) {
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.tapped()),
            Effects.DealDamage(
                amount = DynamicAmounts.powerOf(EffectTarget.IterationEntity),
                target = EffectTarget.IterationEntity,
                damageSource = EffectTarget.IterationEntity,
            ),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "124"
        artist = "Steven Belledin"
        imageUri = "https://cards.scryfall.io/normal/front/2/8/28ea046b-1bbc-4430-a859-321602c9e928.jpg?1783931556"
        ruling(
            "2020-01-24",
            "If The Akroan War leaves the battlefield before its first ability resolves, you won't gain " +
                "control of the creature at all.",
        )
        ruling(
            "2020-01-24",
            "If a creature can't attack for any reason (such as being tapped or having come under that " +
                "player's control that turn), then it doesn't attack. If there's a cost associated with having " +
                "it attack, the player isn't forced to pay that cost, so it doesn't have to attack in that case either.",
        )
        ruling(
            "2020-01-24",
            "Because a Saga isn't sacrificed until after its last ability has resolved, you still control the " +
                "creature you took with The Akroan War's first ability while the last ability is resolving.",
        )
    }
}
