package com.wingedsheep.mtg.sets.definitions.mmq.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Soothing Balm
 * {1}{W}
 * Instant
 * Target player gains 5 life.
 */
val SoothingBalm = card("Soothing Balm") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Target player gains 5 life."

    spell {
        val player = target("target", Targets.Player)
        effect = Effects.GainLife(5, player)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "48"
        artist = "Scott M. Fischer"
        flavorText = "Orim taught Ta-Karnst and the other Cho-Arrim healers a far less invasive method of healing."
        imageUri = "https://cards.scryfall.io/normal/front/9/6/96b8f4be-9f4d-4373-8141-a03518ecd38a.jpg"
    }
}
