package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Frantic Inventory
 * {1}{U}
 * Instant
 *
 * Draw a card, then draw cards equal to the number of cards named Frantic Inventory in your
 * graveyard.
 *
 * The count is read on resolution, while this spell is still on the stack, so it never counts
 * itself (2020-06-23 ruling).
 */
val FranticInventory = card("Frantic Inventory") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Draw a card, then draw cards equal to the number of cards named Frantic Inventory in your graveyard."

    spell {
        effect = Effects.DrawCards(1) then
            Effects.DrawCards(
                DynamicAmounts.count(Player.You, Zone.GRAVEYARD, GameObjectFilter.Any.named("Frantic Inventory"))
            )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "50"
        artist = "Aaron Miller"
        flavorText = "\"Another village, another angry mob. Why do I even bother unpacking?\""
        imageUri = "https://cards.scryfall.io/normal/front/8/f/8f14abb0-0e9f-448e-85d7-6cb71f756c56.jpg?1783930728"
        ruling("2020-06-23", "Because Frantic Inventory is still on the stack while it's resolving, it doesn't count towards the number of cards you draw.")
    }
}
