package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Rumbling Ruin
 * {5}{R}
 * Creature — Elemental
 * 6/6
 * When this creature enters, count the number of +1/+1 counters on creatures you control.
 * Creatures your opponents control with power less than or equal to that number can't block
 * this turn.
 *
 * A floating `CantBlockGroup` whose `powerAtMostDynamic` cap is frozen when the trigger resolves
 * (the group baker rewrites it to a fixed `PowerAtMost`), while which creatures have power that
 * low stays live — exactly the ruling's split.
 */
val RumblingRuin = card("Rumbling Ruin") {
    manaCost = "{5}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Elemental"
    power = 6
    toughness = 6
    oracleText = "When this creature enters, count the number of +1/+1 counters on creatures you control. Creatures your opponents control with power less than or equal to that number can't block this turn."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CantBlockGroup(
            GroupFilter(
                GameObjectFilter.Creature.opponentControls().powerAtMostDynamic(
                    DynamicAmounts.battlefield(Player.You, GameObjectFilter.Creature)
                        .totalCounters(CounterType.PLUS_ONE_PLUS_ONE)
                )
            )
        )
        description = "When this creature enters, count the number of +1/+1 counters on creatures you control. Creatures your opponents control with power less than or equal to that number can't block this turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "112"
        artist = "Jakub Kasper"
        flavorText = "The Rubblebelt grows restless."
        imageUri = "https://cards.scryfall.io/normal/front/7/1/71fe7057-de70-456d-8f33-f64652447bdd.jpg?1783933677"
        ruling("2019-01-25", "The number of +1/+1 counters among creatures you control is counted only as Rumbling Ruin's ability resolves. The set of creatures that can't block is constantly updated as their power changes, but the number against which their power is compared doesn't change later in the turn, even if the number of +1/+1 counters among your creatures changes.")
        ruling("2019-01-25", "Once a creature with high enough power has blocked, changing the power of the blocking creature won't remove the blocking creature from combat or cause the creature it blocked to become unblocked.")
        ruling("2019-01-25", "If there are no +1/+1 counters on creatures you control as Rumbling Ruin's ability resolves, creatures with power 0 or less can't block this turn.")
    }
}
