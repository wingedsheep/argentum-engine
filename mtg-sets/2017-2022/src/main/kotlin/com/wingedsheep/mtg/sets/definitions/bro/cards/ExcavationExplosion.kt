package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Excavation Explosion
 * {2}{R}
 * Sorcery
 * Excavation Explosion deals 3 damage to any target. Create a tapped Powerstone token.
 */
val ExcavationExplosion = card("Excavation Explosion") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Excavation Explosion deals 3 damage to any target. Create a tapped Powerstone token. (It's an artifact with \"{T}: Add {C}. This mana can't be spent to cast a nonartifact spell.\")"

    spell {
        val anyTarget = target(Targets.Any)
        effect = Effects.DealDamage(3, anyTarget) then Effects.CreatePowerstone(1, tapped = true)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "132"
        artist = "Campbell White"
        flavorText = "Urza's engineers could take weeks to dig out a powerstone. The goblins weren't so painstaking."
        imageUri = "https://cards.scryfall.io/normal/front/a/1/a169612e-f5de-4653-bc31-9ede8c5bdc75.jpg?1783920073"
    }
}
