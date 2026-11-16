package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Glade Watcher
 * {1}{G}
 * Creature — Elemental
 * 3/3
 *
 * Defender
 * Formidable — {G}: This creature can attack this turn as though it didn't have defender. Activate
 * only if creatures you control have total power 8 or greater.
 *
 * Hightide Hermit's self defender-lift behind Atarka Beastbreaker's formidable restriction (checked
 * only on activation).
 */
val GladeWatcher = card("Glade Watcher") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elemental"
    oracleText = "Defender\n" +
        "Formidable — {G}: This creature can attack this turn as though it didn't have defender. " +
        "Activate only if creatures you control have total power 8 or greater."
    power = 3
    toughness = 3

    keywords(Keyword.DEFENDER)

    activatedAbility {
        cost = Costs.Mana("{G}")
        effect = Effects.CanAttackDespiteDefenderThisTurn(EffectTarget.Self)
        restrictions = listOf(
            ActivationRestriction.OnlyIfCondition(
                Conditions.CompareAmounts(
                    DynamicAmounts.battlefield(Player.You, GameObjectFilter.Creature).sumPower(),
                    ComparisonOperator.GTE,
                    8
                )
            )
        )
        description = "Formidable — {G}: This creature can attack this turn as though it didn't have defender."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "188"
        artist = "Jesper Ejsing"
        imageUri = "https://cards.scryfall.io/normal/front/0/2/02c9c6d9-e715-4b70-a10e-8bc5a4e9e938.jpg?1783938579"
        ruling("2015-02-25", "If you control a creature with power less than 0, use its actual power when calculating the total power of creatures you control. For example, if you control three creatures with powers 4, 5, and -2, the total power of creatures you control is 7.")
        ruling("2015-02-25", "Some formidable abilities are activated abilities that require creatures you control to have total power 8 or greater. Once you activate these abilities, it doesn't matter what happens to the total power of creatures you control.")
    }
}
