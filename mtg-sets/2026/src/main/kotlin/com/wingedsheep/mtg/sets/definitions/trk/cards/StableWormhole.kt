package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Stable Wormhole
 * {2}{G}
 * Enchantment
 *
 * Whenever you attack, create a tapped Planet land token named New Planet with
 * "{T}: Add one mana of any color."
 */
val StableWormhole = card("Stable Wormhole") {
    manaCost = "{2}{G}"
    typeLine = "Enchantment"
    oracleText = "Whenever you attack, create a tapped Planet land token named New Planet with \"{T}: Add one mana of any color.\""

    triggeredAbility {
        trigger = Triggers.you.attacks()
        effect = Effects.CreateNewPlanet()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "215"
        artist = "Bruce Brenneise"
        flavorText = "Artificial verteron nodes stabilize the Bajoran wormhole, creating a reliable subspace passage to the Gamma Quadrant, shortcutting a journey of several decades."
        imageUri = "https://cards.scryfall.io/normal/front/c/6/c6fc3447-55de-4f5d-8d06-8d9e2ff4d5d0.jpg?1793274281"
    }
}
