package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AttackTax

/**
 * Collective Restraint
 * {3}{U}
 * Enchantment
 * Domain — Creatures can't attack you unless their controller pays {X} for each
 * creature they control that's attacking you, where X is the number of basic land
 * types among lands you control.
 */
val CollectiveRestraint = card("Collective Restraint") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment"
    oracleText = "Domain — Creatures can't attack you unless their controller pays {X} for each creature they control that's attacking you, where X is the number of basic land types among lands you control."

    staticAbility {
        ability = AttackTax(amountPerAttacker = DynamicAmounts.domain())
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "49"
        artist = "Alan Rabinowitz"
        imageUri = "https://cards.scryfall.io/normal/front/d/7/d71daa57-ac02-4dd9-8c90-d38bdd45fb51.jpg?1562938175"
    }
}
