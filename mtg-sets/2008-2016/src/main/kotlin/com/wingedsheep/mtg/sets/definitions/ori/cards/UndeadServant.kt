package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Undead Servant
 * {3}{B}
 * Creature — Zombie
 * 3/2
 * When this creature enters, create a 2/2 black Zombie creature token for each card named
 * Undead Servant in your graveyard.
 */
val UndeadServant = card("Undead Servant") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie"
    power = 3
    toughness = 2
    oracleText = "When this creature enters, create a 2/2 black Zombie creature token for each card named Undead Servant in your graveyard."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateToken(
            count = DynamicAmounts.count(Player.You, Zone.GRAVEYARD, GameObjectFilter.Any.named("Undead Servant")),
            power = 2,
            toughness = 2,
            colors = setOf(Color.BLACK),
            creatureTypes = setOf("Zombie"),
            imageUri = "https://cards.scryfall.io/normal/front/e/5/e5ccae95-95c2-4d11-aa68-5c80ecf90fd2.jpg?1783938294"
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "124"
        artist = "James Zapata"
        flavorText = "\"Get up. You're not finished yet.\"\n—Liliana Vess"
        imageUri = "https://cards.scryfall.io/normal/front/3/6/36afdfd4-8db7-45b6-9b6d-b9293fe6c26d.jpg?1783938335"
    }
}
