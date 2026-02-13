package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.DealDamageToGroupEffect
import com.wingedsheep.sdk.scripting.DealDamageToPlayersEffect
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Skirk Fire Marshal
 * {3}{R}{R}
 * Creature — Goblin
 * 2/2
 * Protection from red
 * Tap five untapped Goblins you control: Skirk Fire Marshal deals 10 damage to each creature and each player.
 */
val SkirkFireMarshal = card("Skirk Fire Marshal") {
    manaCost = "{3}{R}{R}"
    typeLine = "Creature — Goblin"
    power = 2
    toughness = 2
    oracleText = "Protection from red\nTap five untapped Goblins you control: Skirk Fire Marshal deals 10 damage to each creature and each player."

    keywordAbility(KeywordAbility.ProtectionFromColor(Color.RED))

    activatedAbility {
        cost = Costs.TapPermanents(5, GameObjectFilter.Creature.withSubtype("Goblin"))
        effect = DealDamageToGroupEffect(10) then DealDamageToPlayersEffect(10)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "229"
        artist = "Greg Hildebrandt & Tim Hildebrandt"
        flavorText = "He's boss because he's smart enough to get out of the way."
        imageUri = "https://cards.scryfall.io/large/front/b/7/b71117d0-5cf7-4041-b568-00bd8a975dd8.jpg?1562938138"
    }
}
