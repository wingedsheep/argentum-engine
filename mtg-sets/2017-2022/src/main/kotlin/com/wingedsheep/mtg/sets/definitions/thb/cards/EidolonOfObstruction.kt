package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.IncreaseActivatedAbilityCost
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Eidolon of Obstruction
 * {1}{W}
 * Enchantment Creature — Spirit
 * 2/1
 * First strike
 * Loyalty abilities of planeswalkers your opponents control cost {1} more to activate.
 *
 * The tax is Suppression Field's [IncreaseActivatedAbilityCost] narrowed with
 * `loyaltyAbilitiesOnly`: it prepends {1} to the loyalty cost and leaves the loyalty change alone.
 */
val EidolonOfObstruction = card("Eidolon of Obstruction") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment Creature — Spirit"
    power = 2
    toughness = 1
    oracleText = "First strike\nLoyalty abilities of planeswalkers your opponents control cost {1} more to activate."

    keywords(Keyword.FIRST_STRIKE)

    staticAbility {
        ability = IncreaseActivatedAbilityCost(
            filter = GroupFilter(GameObjectFilter.Planeswalker.opponentControls()),
            amount = DynamicAmounts.fixed(1),
            loyaltyAbilitiesOnly = true,
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "12"
        artist = "Jason Rainville"
        flavorText = "Death turned admirable conviction into pointless intransigence."
        imageUri = "https://cards.scryfall.io/normal/front/2/f/2f48f802-df9d-45b2-afea-98699bcdf0d6.jpg?1783931599"
        ruling("2020-01-24", "Eidolon of Obstruction doesn't replace the loyalty cost of activating a loyalty ability or cause it to cost more loyalty to activate. It just adds a mana component to the ability's activation cost.")
        ruling("2020-01-24", "Loyalty abilities cost just {1} more to activate, no matter how much loyalty they cost to activate.")
    }
}
