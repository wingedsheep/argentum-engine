package com.wingedsheep.mtg.sets.definitions.rna.cards

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
 * Titanic Brawl
 * {1}{G}
 * Instant
 * This spell costs {1} less to cast if it targets a creature you control with a +1/+1 counter
 * on it.
 * Target creature you control fights target creature you don't control.
 *
 * Swift Kick's fight between a creature you control and one you don't, plus Run Over's
 * [CostReductionSource.FixedIfAnyTargetMatches] self-cost reduction keyed on a
 * creature-you-control-with-a-+1/+1-counter filter.
 */
val TitanicBrawl = card("Titanic Brawl") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "This spell costs {1} less to cast if it targets a creature you control with a +1/+1 counter on it.\n" +
        "Target creature you control fights target creature you don't control. " +
        "(Each deals damage equal to its power to the other.)"

    spell {
        val yours = target(TargetFilter(GameObjectFilter.Creature.youControl()))
        val theirs = target(
            TargetFilter(
                GameObjectFilter.Creature.copy(
                    controllerPredicate = ControllerPredicate.Not(ControllerPredicate.ControlledByYou)
                )
            )
        )
        effect = Effects.Fight(yours, theirs)
    }

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGenericBy(
                CostReductionSource.FixedIfAnyTargetMatches(
                    amount = 1,
                    filter = GameObjectFilter.Creature.withCounter(CounterType.PLUS_ONE_PLUS_ONE).youControl(),
                ),
            ),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "146"
        artist = "Svetlin Velinov"
        flavorText = "Whoever wins, the neighborhood loses."
        imageUri = "https://cards.scryfall.io/normal/front/b/c/bcf9b57a-a759-4488-965a-651070cd2156.jpg?1783933664"
    }
}
