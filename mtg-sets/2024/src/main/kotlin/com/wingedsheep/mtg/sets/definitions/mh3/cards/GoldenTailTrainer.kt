package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.predicates.StatePredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Golden-Tail Trainer (MH3 #187)
 * {1}{G}{W}
 * Creature — Fox Samurai
 * 1/3
 * Aura and Equipment spells you cast cost {X} less to cast, where X is this creature's power.
 * Whenever this creature attacks, other modified creatures you control get +X/+X until end of
 * turn, where X is this creature's power.
 *
 * The discount is the Scarlet Witch shape: a battlefield [SpellCostTarget.YouCast] reduction whose
 * amount is [DynamicAmounts.sourcePower] (projected, so counters and pumps raise it). "Modified" is
 * [StatePredicate.IsModified] (CR 700.9) on an "other creatures you control" group.
 */
val GoldenTailTrainer = card("Golden-Tail Trainer") {
    manaCost = "{1}{G}{W}"
    colorIdentity = "GW"
    typeLine = "Creature — Fox Samurai"
    power = 1
    toughness = 3
    oracleText = "Aura and Equipment spells you cast cost {X} less to cast, where X is this creature's power.\n" +
        "Whenever this creature attacks, other modified creatures you control get +X/+X until end of turn, " +
        "where X is this creature's power. (Equipment, Auras you control, and counters are modifications.)"

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.YouCast(GameObjectFilter.Any.withAnySubtype("Aura", "Equipment")),
            modification = CostModification.ReduceGenericBy(
                CostReductionSource.Dynamic(DynamicAmounts.sourcePower())
            ),
        )
    }

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.ForEachInGroup(
            filter = GroupFilter(
                GameObjectFilter.Creature.youControl().withStatePredicate(StatePredicate.IsModified),
                excludeSelf = true
            ),
            effect = Effects.ModifyStats(
                power = DynamicAmounts.sourcePower(),
                toughness = DynamicAmounts.sourcePower(),
                target = EffectTarget.IterationEntity,
                duration = Duration.EndOfTurn
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "187"
        artist = "Nino Vecia"
        imageUri = "https://cards.scryfall.io/normal/front/2/7/27d42bd9-0307-42ec-a4cd-b39b69b607d0.jpg?1783911251"
    }
}
