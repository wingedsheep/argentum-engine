package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Clattering Skeletons
 * {3}{B}
 * Creature — Skeleton
 * 4/3
 * When this creature dies, venture into the dungeon. (Enter the first room or advance to the next room.)
 */
val ClatteringSkeletons = card("Clattering Skeletons") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Skeleton"
    oracleText = "When this creature dies, venture into the dungeon. (Enter the first room or advance to the next room.)"
    power = 4
    toughness = 3

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.VentureIntoTheDungeon()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "93"
        artist = "Aleksi Briclot"
        flavorText = "Those who die in the dungeon become its guardians."
        imageUri = "https://cards.scryfall.io/normal/front/0/9/09ac0b2b-d4d3-4977-b241-7788239362de.jpg?1783926499"
    }
}
