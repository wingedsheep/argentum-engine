package com.wingedsheep.mtg.sets.definitions.fin.cards

import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostGating
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.conditions.Compare
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.effects.TransformEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Serah Farron // Crystallized Serah — Final Fantasy #240
 * {1}{G}{W} · Legendary Creature — Human Citizen 2/2 // Legendary Artifact
 *
 * Front — Serah Farron:
 *   The first legendary creature spell you cast each turn costs {2} less to cast.
 *   At the beginning of combat on your turn, if you control two or more other legendary
 *   creatures, you may transform Serah Farron.
 *
 * Back — Crystallized Serah:
 *   The first legendary creature spell you cast each turn costs {2} less to cast.
 *   Legendary creatures you control get +2/+2.
 *
 * The transform trigger is an intervening-"if" (checked when the trigger would go on the
 * stack and again on resolution) over the count of *other* legendary creatures you control —
 * `AggregateBattlefield(excludeSelf = true)` so Serah never counts herself even if her own
 * types change. The cost reduction is `CostGating.NthOfTypePerTurn(1)` (Eluge-style
 * first-matching-spell-each-turn gating) and, per the official ruling, only reduces the
 * generic component of the cost.
 */
private val CrystallizedSerah = card("Crystallized Serah") {
    manaCost = ""
    colorIdentity = "GW"
    typeLine = "Legendary Artifact"
    oracleText = "The first legendary creature spell you cast each turn costs {2} less to cast.\n" +
        "Legendary creatures you control get +2/+2."

    // The first legendary creature spell you cast each turn costs {2} less to cast.
    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.YouCast(GameObjectFilter.Creature.legendary()),
            modification = CostModification.ReduceGeneric(2),
            gating = CostGating.NthOfTypePerTurn(1),
        )
    }

    // Legendary creatures you control get +2/+2.
    staticAbility {
        ability = ModifyStats(
            powerBonus = 2,
            toughnessBonus = 2,
            filter = GroupFilter(GameObjectFilter.Creature.legendary().youControl()),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "240"
        artist = "Carissa Susilo"
        imageUri = "https://cards.scryfall.io/normal/back/6/2/62fa74c0-43ae-445c-8039-ca9d00e9709a.jpg?1782686410"
    }
}

private val SerahFarronFront = card("Serah Farron") {
    manaCost = "{1}{G}{W}"
    colorIdentity = "GW"
    typeLine = "Legendary Creature — Human Citizen"
    oracleText = "The first legendary creature spell you cast each turn costs {2} less to cast.\n" +
        "At the beginning of combat on your turn, if you control two or more other legendary " +
        "creatures, you may transform Serah Farron."
    power = 2
    toughness = 2

    // The first legendary creature spell you cast each turn costs {2} less to cast.
    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.YouCast(GameObjectFilter.Creature.legendary()),
            modification = CostModification.ReduceGeneric(2),
            gating = CostGating.NthOfTypePerTurn(1),
        )
    }

    // At the beginning of combat on your turn, if you control two or more other legendary
    // creatures, you may transform Serah Farron.
    triggeredAbility {
        trigger = Triggers.BeginCombat
        triggerCondition = Compare(
            DynamicAmount.AggregateBattlefield(
                player = Player.You,
                filter = GameObjectFilter.Creature.legendary(),
                excludeSelf = true,
            ),
            ComparisonOperator.GTE,
            DynamicAmount.Fixed(2),
        )
        effect = MayEffect(effect = TransformEffect(EffectTarget.Self))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "240"
        artist = "Carissa Susilo"
        imageUri = "https://cards.scryfall.io/normal/front/6/2/62fa74c0-43ae-445c-8039-ca9d00e9709a.jpg?1782686410"

        ruling(
            "2025-06-06",
            "The cost reduction applies only to generic mana in the total cost of legendary " +
                "creature spells you cast."
        )
        ruling(
            "2025-06-06",
            "Serah Farron's last ability checks at the moment it would trigger to see if you " +
                "control two or more other legendary creatures. If you don't, the ability won't " +
                "trigger at all. If it does trigger, the ability will check again as it tries to " +
                "resolve."
        )
    }
}

val SerahFarron: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = SerahFarronFront,
    backFace = CrystallizedSerah,
)
