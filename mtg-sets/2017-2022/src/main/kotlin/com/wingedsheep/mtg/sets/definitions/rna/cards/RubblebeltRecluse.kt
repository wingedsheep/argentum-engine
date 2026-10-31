package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.MustAttack

/**
 * Rubblebelt Recluse (RNA #111)
 * {4}{R}  Creature — Ogre Berserker  6/5
 *
 * This creature attacks each combat if able.
 */
val RubblebeltRecluse = card("Rubblebelt Recluse") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Ogre Berserker"
    oracleText = "This creature attacks each combat if able."
    power = 6
    toughness = 5

    staticAbility {
        ability = MustAttack()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "111"
        artist = "Zoltan Boros"
        flavorText = "Joining a guild, even the Gruul, would mean giving up some of his independence. He'd rather smash what he wants to smash."
        imageUri = "https://cards.scryfall.io/normal/front/2/f/2faac11b-4ece-4537-aa55-c2d0afa41786.jpg"
    }
}
