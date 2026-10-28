package com.wingedsheep.mtg.sets.definitions.dis.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Vision Skeins
 * {1}{U}
 * Instant
 * Each player draws two cards.
 *
 * Every player draws in APNAP order (CR 101.4).
 */
val VisionSkeins = card("Vision Skeins") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Each player draws two cards."

    spell {
        effect = Effects.ForEachPlayer(Player.ActivePlayerFirst, Effects.DrawCards(2))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "36"
        artist = "Aleksi Briclot"
        flavorText = "\"I could see in the other mage's eyes that he'd thought of it too. Then it became a race to exploit the knowledge first.\""
        imageUri = "https://cards.scryfall.io/normal/front/8/e/8e632566-1f58-4439-983a-a25c06b7196c.jpg?1783943433"
    }
}
