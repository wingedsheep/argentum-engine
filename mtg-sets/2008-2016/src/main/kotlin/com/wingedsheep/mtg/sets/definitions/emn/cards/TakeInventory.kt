package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Take Inventory
 * {1}{U}
 * Sorcery
 *
 * Draw a card, then draw cards equal to the number of cards named Take Inventory in your graveyard.
 *
 * The count is read on resolution, while this spell is still on the stack, so it never counts
 * itself (2016-07-13 ruling).
 */
val TakeInventory = card("Take Inventory") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Draw a card, then draw cards equal to the number of cards named Take Inventory in your graveyard."

    spell {
        effect = Effects.DrawCards(1) then
            Effects.DrawCards(
                DynamicAmounts.count(Player.You, Zone.GRAVEYARD, GameObjectFilter.Any.named("Take Inventory"))
            )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "76"
        artist = "Greg Staples"
        flavorText = "A place for everything, and everything in its place."
        imageUri = "https://cards.scryfall.io/normal/front/a/3/a3584150-4f09-4c15-b13e-707bd91b3004.jpg?1783937491"
        ruling("2016-07-13", "Because Take Inventory is still on the stack as it's resolving, that spell card isn't in your graveyard and won't affect the number of cards you draw.")
    }
}
