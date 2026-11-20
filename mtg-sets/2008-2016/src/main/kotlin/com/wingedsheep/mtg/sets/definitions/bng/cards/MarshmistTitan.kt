package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget

/**
 * Marshmist Titan — Born of the Gods #76
 * {6}{B} · Creature — Giant · 4/5
 *
 * This spell costs {X} less to cast, where X is your devotion to black.
 *
 * Drag to the Underworld's shape: a self-cast [CostModification.ReduceGenericBy] fed
 * [DynamicAmounts.devotionTo] black. With no source permanent "you" is the caster, devotion is read
 * when the total cost is determined, and only generic mana is reduced — at devotion seven or more
 * the Titan costs {B} (ruling).
 */
val MarshmistTitan = card("Marshmist Titan") {
    manaCost = "{6}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Giant"
    power = 4
    toughness = 5
    oracleText = "This spell costs {X} less to cast, where X is your devotion to black. (Each {B} in " +
        "the mana costs of permanents you control counts toward your devotion to black.)"

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGenericBy(
                CostReductionSource.Dynamic(DynamicAmounts.devotionTo(Color.BLACK)),
            ),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "76"
        artist = "Volkan Baǵa"
        flavorText = "A favorite of Erebos, for it has sent many to the Underworld."
        imageUri = "https://cards.scryfall.io/normal/front/0/9/093d2e4c-f2c4-4d4e-a1b7-200327ce23b0.jpg?1783939554"
        ruling(
            "2014-02-01",
            "Your devotion to black is calculated when you determine Marshmist Titan's total cost, and " +
                "that cost is locked in before any costs are paid. For example, if you control a creature " +
                "with black mana symbols in its mana cost that can be sacrificed for mana, those mana " +
                "symbols will count toward your devotion to black. You can then sacrifice that creature " +
                "for mana to pay the reduced total cost."
        )
        ruling(
            "2014-02-01",
            "If your devotion to black is greater than six, Marshmist Titan will cost {B} to cast. " +
                "The colored mana requirement isn't reduced."
        )
    }
}
