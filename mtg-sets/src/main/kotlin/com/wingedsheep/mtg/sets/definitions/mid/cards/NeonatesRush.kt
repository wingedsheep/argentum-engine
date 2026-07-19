package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Neonate's Rush
 * {2}{R}
 * Instant
 *
 * This spell costs {1} less to cast if you control a Vampire.
 * Neonate's Rush deals 1 damage to target creature and 1 damage to its controller. Draw a card.
 *
 * Cost reduction is a [ModifySpellCost] self-cast [CostReductionSource.FixedIfControlFilter]
 * (Venom's Hunger pattern). The two damage clauses share one target: 1 to the creature and 1 to
 * its controller via [EffectTarget.TargetController] (Judgment Bolt pattern).
 */
val NeonatesRush = card("Neonate's Rush") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "This spell costs {1} less to cast if you control a Vampire.\n" +
        "Neonate's Rush deals 1 damage to target creature and 1 damage to its controller. Draw a card."

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGenericBy(
                CostReductionSource.FixedIfControlFilter(
                    amount = 1,
                    filter = GameObjectFilter.Creature.withSubtype(Subtype.VAMPIRE)
                )
            )
        )
    }

    spell {
        val creature = target("target creature", Targets.Creature)
        effect = Effects.DealDamage(1, creature)
            .then(Effects.DealDamage(1, EffectTarget.TargetController))
            .then(Effects.DrawCards(1))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "151"
        artist = "Justine Cruz"
        flavorText = "\"We spend our entire existence pursuing the joy of our first night's feast.\"\n—Anje Falkenrath"
        imageUri = "https://cards.scryfall.io/normal/front/d/e/dee17e12-e08f-4449-9f49-05f20e0d1670.jpg?1783925594"
    }
}
