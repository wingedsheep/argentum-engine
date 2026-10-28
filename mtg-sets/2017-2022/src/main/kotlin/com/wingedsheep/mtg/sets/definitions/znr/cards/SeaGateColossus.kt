package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget

/**
 * Sea Gate Colossus — Zendikar Rising #251
 * {7} · Artifact Creature — Golem Warrior · 7/5
 *
 * This spell costs {1} less to cast for each creature in your party.
 *
 * The Deadly Alliance self-cast [ModifySpellCost] reading [DynamicAmounts.partySize]; a full party
 * casts it for {3}. Its own Warrior type doesn't count — it isn't on the battlefield while cast.
 */
val SeaGateColossus = card("Sea Gate Colossus") {
    manaCost = "{7}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Golem Warrior"
    oracleText = "This spell costs {1} less to cast for each creature in your party. (Your party consists of " +
        "up to one each of Cleric, Rogue, Warrior, and Wizard.)"
    power = 7
    toughness = 5

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGenericBy(
                CostReductionSource.Dynamic(DynamicAmounts.partySize()),
            ),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "251"
        artist = "Daniel Ljunggren"
        flavorText = "\"When you are battered and exhausted, look up and be inspired by its resolve.\"\n—Tazri"
        imageUri = "https://cards.scryfall.io/normal/front/b/0/b04fd8e8-ee8d-4cb7-87ab-955303c46ece.jpg?1783929312"
        ruling(
            "2020-09-25",
            "To determine the total cost of a spell, start with the mana cost or alternative cost you're " +
                "paying, add any cost increases, then apply any cost reductions. The mana value of the spell " +
                "is determined only by its mana cost, no matter what the total cost to cast the spell was."
        )
    }
}
