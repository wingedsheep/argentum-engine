package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Veteran Dungeoneer
 * {3}{W}
 * Creature — Human Warrior
 * 3/4
 * When this creature enters, venture into the dungeon. (Enter the first room or advance to the next room.)
 */
val VeteranDungeoneer = card("Veteran Dungeoneer") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Warrior"
    power = 3
    toughness = 4
    oracleText = "When this creature enters, venture into the dungeon. (Enter the first room or advance to the next room.)"

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.VentureIntoTheDungeon()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "40"
        artist = "Sebastian Giacobino"
        imageUri = "https://cards.scryfall.io/normal/front/d/8/d8fe33b3-afaf-4a98-ae64-7d7e0eabe993.jpg?1783926524"
    }
}
