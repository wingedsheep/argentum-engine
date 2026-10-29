package com.wingedsheep.mtg.sets.definitions.cmr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Prava of the Steel Legion — Commander Legends #38
 * {2}{W} · Legendary Creature — Cat Soldier · 1/4
 *
 * During your turn, creature tokens you control get +1/+4.
 * {3}{W}: Create a 1/1 white Soldier creature token.
 * Partner
 *
 * The anthem is a conditional static (the Mite Overseer shape), re-read on every projection, so
 * it switches on and off with whose turn it is. Partner is deck construction only.
 */
val PravaOfTheSteelLegion = card("Prava of the Steel Legion") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Cat Soldier"
    power = 1
    toughness = 4
    oracleText = "During your turn, creature tokens you control get +1/+4.\n" +
        "{3}{W}: Create a 1/1 white Soldier creature token.\n" +
        "Partner (You can have two commanders if both have partner.)"

    staticAbility {
        condition = Conditions.IsYourTurn
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 4,
            filter = GroupFilter(GameObjectFilter.Creature.token().youControl()),
        )
    }

    activatedAbility {
        cost = Costs.Mana("{3}{W}")
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Soldier"),
            imageUri = "https://cards.scryfall.io/normal/front/4/3/430ed737-b918-4485-a623-e781c0beb67b.jpg?1783928591",
        )
        description = "{3}{W}: Create a 1/1 white Soldier creature token."
    }

    keywordAbility(KeywordAbility.partner())

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "38"
        artist = "Matt Stewart"
        imageUri = "https://cards.scryfall.io/normal/front/e/9/e9e40e2a-e447-4754-a98b-5521e546781f.jpg?1783928877"
        ruling(
            "2020-11-10",
            "The bonus provided by Prava's first ability will still be in effect when damage is removed " +
                "during the cleanup step."
        )
        ruling(
            "2020-11-10",
            "However, if Prava leaves the battlefield, its bonus immediately stops applying. Nonlethal damage " +
                "dealt to creature tokens you control may become lethal if Prava leaves the battlefield during " +
                "your turn."
        )
    }
}
