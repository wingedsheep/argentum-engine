package com.wingedsheep.mtg.sets.definitions.j25.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Delightful Discovery — Foundations Jumpstart #6 (canonical printing)
 * {4}{U} · Instant
 *
 * This spell costs {1} less to cast for each spell your opponents have cast this turn.
 * Scry 2, then draw two cards.
 *
 * The discount is a `SelfCast` `ReduceGenericBy` over the opponents' cast history this turn
 * (`spellsCastThisTurn(Player.EachOpponent)`, resolved against the caster). Only the generic part
 * is reduced, so the cost never drops below {U} (the ruling).
 */
val DelightfulDiscovery = card("Delightful Discovery") {
    manaCost = "{4}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "This spell costs {1} less to cast for each spell your opponents have cast this turn.\n" +
        "Scry 2, then draw two cards. (To scry 2, look at the top two cards of your library, then put " +
        "any number of them on the bottom and the rest on top in any order.)"

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGenericBy(
                CostReductionSource.Dynamic(DynamicAmounts.spellsCastThisTurn(Player.EachOpponent)),
            ),
        )
    }

    spell {
        effect = Effects.Scry(2) then Effects.DrawCards(2)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "6"
        artist = "Manuel Castañón"
        flavorText = "\"What do you think? New class pet?\""
        imageUri = "https://cards.scryfall.io/normal/front/d/e/dee342ec-f546-41b7-add5-0a85f5c8bc57.jpg?1783908870"
        ruling(
            "2024-11-08",
            "To determine the total cost of a spell, start with the mana cost or alternative cost you're " +
                "paying, add any cost increases, then apply any cost reductions. The mana value of the " +
                "spell remains unchanged, no matter what the total cost to cast it was."
        )
        ruling("2024-11-08", "Delightful Discovery's first ability can't reduce its cost below {U}.")
    }
}
