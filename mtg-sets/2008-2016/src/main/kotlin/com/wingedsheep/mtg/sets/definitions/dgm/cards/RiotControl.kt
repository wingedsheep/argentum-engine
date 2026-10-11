package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Riot Control
 * {2}{W}
 * Instant
 * You gain 1 life for each creature your opponents control. Prevent all damage that would be dealt
 * to you this turn.
 */
val RiotControl = card("Riot Control") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "You gain 1 life for each creature your opponents control. " +
        "Prevent all damage that would be dealt to you this turn."

    spell {
        effect = Effects.GainLife(DynamicAmounts.battlefield(Player.EachOpponent, GameObjectFilter.Creature).count()) then
            Effects.PreventDamage()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "6"
        artist = "Slawomir Maniak"
        flavorText = "\"Ravnicans need order. If they don't have it, we shall bring it to them.\"\n—Isperia"
        imageUri = "https://cards.scryfall.io/normal/front/d/7/d7886607-86db-4221-8752-296104aaaef2.jpg"
    }
}
