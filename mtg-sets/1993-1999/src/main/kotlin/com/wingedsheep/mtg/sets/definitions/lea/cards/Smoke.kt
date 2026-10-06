package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.UntapLimitPerStep

// The Damping Field / Winter Orb global untap-count cap, over creatures.
val Smoke = card("Smoke") {
    manaCost = "{R}{R}"
    typeLine = "Enchantment"
    oracleText = "Players can't untap more than one creature during their untap steps."

    staticAbility {
        ability = UntapLimitPerStep(GameObjectFilter.Creature, 1)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "175"
        artist = "Jesper Myrfors"
        imageUri = "https://cards.scryfall.io/normal/front/7/c/7c67788e-d713-47c3-ab9f-b8a6212ae24f.jpg?1783948681"
        ruling("2004-10-04", "Animated lands are affected by this spell. If on the battlefield with an effect that limits the number of land you untap, untapping an animated land will count as the one creature and the one land you can untap... thereby limiting you to one thing to be untapped.")
    }
}
