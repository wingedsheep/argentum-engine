package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget

/**
 * Shatterskull Minotaur — Zendikar Rising #160
 * {4}{R}{R} · Creature — Minotaur Warrior · 5/4
 *
 * This spell costs {1} less to cast for each creature in your party.
 * Haste
 *
 * The discount is the same self [ModifySpellCost] reading [DynamicAmounts.partySize] that Tazri,
 * Beacon of Unity uses; it only removes generic mana, so the floor is {R}{R}.
 */
val ShatterskullMinotaur = card("Shatterskull Minotaur") {
    manaCost = "{4}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Minotaur Warrior"
    oracleText = "This spell costs {1} less to cast for each creature in your party. (Your party consists of " +
        "up to one each of Cleric, Rogue, Warrior, and Wizard.)\nHaste"
    power = 5
    toughness = 4

    keywords(Keyword.HASTE)

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGenericBy(
                CostReductionSource.Dynamic(DynamicAmounts.partySize())
            ),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "160"
        artist = "Aleksi Briclot"
        flavorText = "\"We just pay him and point him at problems.\"\n—Aro, Sea Gate adventurer"
        imageUri = "https://cards.scryfall.io/normal/front/9/d/9d311f5f-ad68-48d4-aa5d-66916034ddb9.jpg?1783929352"
    }
}
