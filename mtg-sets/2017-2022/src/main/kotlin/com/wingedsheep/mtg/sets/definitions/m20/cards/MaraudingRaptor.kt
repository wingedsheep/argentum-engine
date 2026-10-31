package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Marauding Raptor — Core Set 2020 #150
 * {1}{R} · Creature — Dinosaur · 2/3
 *
 * Creature spells you cast cost {1} less to cast.
 * Whenever another creature you control enters, this creature deals 2 damage to it. If a Dinosaur
 * is dealt damage this way, this creature gets +2/+0 until end of turn.
 *
 * The cost reduction is a battlefield static ([ModifySpellCost] over [SpellCostTarget.YouCast]), so
 * it never reduces the Raptor's own cost (ruling). The trigger follows Provoke the Trolls' shape:
 * the damage records what it really dealt (`damageDealtVariable`, after prevention), and the pump
 * needs both at least 1 damage dealt and the *damaged creature* — the triggering entity, not the
 * Raptor — to be a Dinosaur. The subtype check runs right after the damage, before state-based
 * actions, so a Dinosaur the 2 damage kills still counts.
 */
val MaraudingRaptor = card("Marauding Raptor") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Dinosaur"
    power = 2
    toughness = 3
    oracleText = "Creature spells you cast cost {1} less to cast.\n" +
        "Whenever another creature you control enters, this creature deals 2 damage to it. " +
        "If a Dinosaur is dealt damage this way, this creature gets +2/+0 until end of turn."

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.YouCast(GameObjectFilter.Creature),
            modification = CostModification.ReduceGenericBy(CostReductionSource.Fixed(1)),
        )
    }

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature.youControl()).enters()
        effect = Effects.Pipeline {
            val dealt = runStoringNumber {
                Effects.DealDamage(2, EffectTarget.TriggeringEntity, damageDealtVariable = it)
            }
            run(
                Effects.If(
                    condition = Conditions.All(
                        Conditions.CompareAmounts(dealt.amount, ComparisonOperator.GTE, 1),
                        Conditions.EntityMatches(
                            EffectTarget.TriggeringEntity,
                            GameObjectFilter.Creature.withSubtype("Dinosaur")
                        )
                    ),
                    then = Effects.ModifyStats(2, 0, EffectTarget.Self)
                )
            )
        }
        description = "Whenever another creature you control enters, this creature deals 2 damage to it. " +
            "If a Dinosaur is dealt damage this way, this creature gets +2/+0 until end of turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "150"
        artist = "Bayard Wu"
        flavorText = "\"Stand still and try not to look like prey!\"\n—Skerk Hobnett, wilderness guide"
        imageUri = "https://cards.scryfall.io/normal/front/6/4/64e4d2ee-e5e5-48e6-a7b8-9045dc8b10a7.jpg?1783932975"
        ruling(
            "2019-07-12",
            "Effects that reduce the generic mana cost of a spell (such as that of Marauding Raptor) can't " +
                "reduce that spell's colored mana requirements."
        )
        ruling("2019-07-12", "Marauding Raptor's cost reduction doesn't affect itself.")
    }
}
