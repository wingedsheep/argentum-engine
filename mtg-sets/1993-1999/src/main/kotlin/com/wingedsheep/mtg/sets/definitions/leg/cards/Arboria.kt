package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantAttackUnless
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Arboria
 * {2}{G}{G}
 * World Enchantment
 *
 * Creatures can't attack a player unless that player cast a spell or put a nontoken permanent
 * onto the battlefield during their last turn.
 *
 * "That player" is the defending player; planeswalkers stay attackable (2022-12-08 ruling). The world
 * supertype puts it under the world rule (CR 704.5k).
 */
val Arboria = card("Arboria") {
    manaCost = "{2}{G}{G}"
    colorIdentity = "G"
    typeLine = "World Enchantment"
    oracleText = "Creatures can't attack a player unless that player cast a spell or put a nontoken " +
        "permanent onto the battlefield during their last turn."

    staticAbility {
        ability = CantAttackUnless(
            condition = Conditions.Any(
                Conditions.CompareAmounts(
                    DynamicAmounts.spellsCastDuringTheirLastTurn(Player.DefendingPlayer),
                    ComparisonOperator.GTE, 1
                ),
                Conditions.CompareAmounts(
                    DynamicAmounts.nontokenPermanentsPutDuringTheirLastTurn(Player.DefendingPlayer),
                    ComparisonOperator.GTE, 1
                )
            ),
            filter = GroupFilter.AllCreatures,
            coversPlaneswalkers = false
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "174"
        artist = "Daniel Gelon"
        imageUri = "https://cards.scryfall.io/normal/front/0/9/095078b0-0f26-442f-9d3b-45e30cdb33c4.jpg?1783948051"
    }
}
