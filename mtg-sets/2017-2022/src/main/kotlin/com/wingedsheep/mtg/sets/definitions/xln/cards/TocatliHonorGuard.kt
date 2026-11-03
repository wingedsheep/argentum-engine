package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.SuppressZoneChangeTriggers

/**
 * Tocatli Honor Guard
 * {1}{W}
 * Creature — Human Soldier
 * 1/3
 * Creatures entering don't cause abilities to trigger.
 *
 * Same static as Torpor Orb: [SuppressZoneChangeTriggers] (default `filter = Creature`).
 */
val TocatliHonorGuard = card("Tocatli Honor Guard") {
    manaCost = "{1}{W}"
    typeLine = "Creature — Human Soldier"
    power = 1
    toughness = 3
    oracleText = "Creatures entering don't cause abilities to trigger."

    staticAbility {
        ability = SuppressZoneChangeTriggers()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "42"
        artist = "Sara Winters"
        flavorText = "The glorious city of Pachatupa is the center of the Sun Empire, and Tocatli, the emperor's palace, is its glowing heart."
        imageUri = "https://cards.scryfall.io/normal/front/a/3/a3fc3501-e7bd-4589-98c5-4476258842ea.jpg"
    }
}
