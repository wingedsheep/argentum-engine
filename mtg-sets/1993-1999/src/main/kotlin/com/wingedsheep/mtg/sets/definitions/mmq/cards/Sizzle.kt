package com.wingedsheep.mtg.sets.definitions.mmq.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Sizzle
 * {2}{R}
 * Sorcery
 * Sizzle deals 3 damage to each opponent.
 */
val Sizzle = card("Sizzle") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Sizzle deals 3 damage to each opponent."

    spell {
        effect = Effects.DealDamage(3, EffectTarget.PlayerRef(Player.EachOpponent))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "213"
        artist = "Brian Snõddy"
        flavorText = "Explosions ripped through the ship overhead, and those unfortunate enough to be directly below scrambled to find cover."
        imageUri = "https://cards.scryfall.io/normal/front/f/1/f1ca1eee-d97d-48c6-84f1-7d1f972c3ca9.jpg"
    }
}
