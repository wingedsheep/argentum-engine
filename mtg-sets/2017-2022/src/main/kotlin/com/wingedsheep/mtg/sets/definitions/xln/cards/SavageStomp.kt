package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate

/**
 * Savage Stomp — Ixalan #205
 * {2}{G} · Sorcery
 *
 * This spell costs {2} less to cast if it targets a Dinosaur you control.
 * Put a +1/+1 counter on target creature you control. Then that creature fights target creature
 * you don't control.
 *
 * The discount is the Mascot Interception shape: [SpellCostTarget.SelfCast] over
 * [CostReductionSource.FixedIfAnyTargetMatches] keyed on a Dinosaur you control (the caster is the
 * filter's "you"), taking {2} off the generic part only. The spell is Struggle for Skemfar's
 * counter-then-fight with a mandatory second target.
 */
val SavageStomp = card("Savage Stomp") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "This spell costs {2} less to cast if it targets a Dinosaur you control.\n" +
        "Put a +1/+1 counter on target creature you control. Then that creature fights target " +
        "creature you don't control. (Each deals damage equal to its power to the other.)"

    spell {
        val mine = target(TargetFilter.CreatureYouControl)
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

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGenericBy(
                CostReductionSource.FixedIfAnyTargetMatches(
                    amount = 2,
                    filter = GameObjectFilter.Creature.withSubtype("Dinosaur").youControl().onBattlefield(),
                ),
            ),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "205"
        artist = "Wayne Reynolds"
        imageUri = "https://cards.scryfall.io/normal/front/c/b/cb862587-8821-43c7-ad08-2b7d2b82cc71.jpg?1783935719"
        ruling(
            "2017-09-29",
            "You can't cast Savage Stomp unless you choose both a creature you control and a creature " +
                "you don't control as targets."
        )
        ruling(
            "2017-09-29",
            "If either target is an illegal target as Savage Stomp resolves, neither creature will deal " +
                "or be dealt damage."
        )
        ruling(
            "2017-09-29",
            "If the creature you control is an illegal target as Savage Stomp tries to resolve, you won't " +
                "put a +1/+1 counter on it. If that creature is a legal target but the other creature " +
                "isn't, you'll still put the counter on the creature you control."
        )
    }
}
