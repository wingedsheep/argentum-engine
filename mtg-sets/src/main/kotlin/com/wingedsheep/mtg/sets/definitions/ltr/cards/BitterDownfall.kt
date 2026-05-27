package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.predicates.StatePredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Bitter Downfall
 * {3}{B}
 * Instant
 * This spell costs {3} less to cast if it targets a creature that was dealt damage this turn.
 * Destroy target creature. Its controller loses 2 life.
 */
val BitterDownfall = card("Bitter Downfall") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "This spell costs {3} less to cast if it targets a creature that was dealt damage this turn.\nDestroy target creature. Its controller loses 2 life."

    spell {
        val creature = target("target creature", Targets.Creature)
        effect = Effects.Destroy(creature)
            .then(Effects.LoseLife(2, EffectTarget.TargetController))
    }

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGenericBy(
                CostReductionSource.FixedIfAnyTargetMatches(
                    amount = 3,
                    filter = GameObjectFilter.Creature.copy(
                        statePredicates = listOf(StatePredicate.WasDealtDamageThisTurn),
                    ),
                ),
            ),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "77"
        artist = "John Di Giovanni"
        flavorText = "\"I tried to take the Ring from Frodo. I am sorry. I have paid.\"\n—Boromir"
        imageUri = "https://cards.scryfall.io/normal/front/e/4/e4b83aa1-33ce-4b8d-ae5a-72f64eef5f09.jpg?1686968375"
    }
}
