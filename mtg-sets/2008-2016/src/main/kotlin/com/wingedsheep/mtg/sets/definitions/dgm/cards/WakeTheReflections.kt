package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Wake the Reflections
 * {W}
 * Sorcery
 * Populate. (Create a token that's a copy of a creature token you control.)
 *
 * Populate chooses — never targets — a creature token you control on resolution, so the spell has
 * no targets and does nothing when you control no creature token: [Effects.CreateTokenCopyOfChosenPermanent]
 * over creature tokens, as on Scion of Vitu-Ghazi and Muster the Departed.
 *
 * Canonical printing: Dragon's Maze, the card's earliest real printing.
 */
val WakeTheReflections = card("Wake the Reflections") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Populate. (Create a token that's a copy of a creature token you control.)"

    spell {
        effect = Effects.CreateTokenCopyOfChosenPermanent(GameObjectFilter.Creature.token())
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "10"
        artist = "Cynthia Sheppard"
        flavorText = "\"You see a bird, an oak, a packbeast. I see the potential for an army.\"\n—Lalia, Selesnya dryad"
        imageUri = "https://cards.scryfall.io/normal/front/3/d/3db0074c-95cf-4d15-8fe1-7282803ec757.jpg?1783940043"
    }
}
