package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Larder Zombie
 * {U}
 * Creature — Zombie
 * 1/3
 * Defender
 * Tap three untapped creatures you control: Surveil 1.
 *
 * The cost has no `{T}` symbol, so Larder Zombie can be one of the three creatures tapped and
 * summoning sickness doesn't gate any of them (same shape as Siege Zombie).
 */
val LarderZombie = card("Larder Zombie") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Creature — Zombie"
    oracleText = "Defender\n" +
        "Tap three untapped creatures you control: Surveil 1. (Look at the top card of your library. You may put that card into your graveyard.)"
    power = 1
    toughness = 3

    keywords(Keyword.DEFENDER)

    activatedAbility {
        cost = Costs.TapPermanents(3)
        effect = Effects.Surveil(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "58"
        artist = "E. M. Gist"
        flavorText = "The cathars arrived at the abandoned lab to find many of the unfinished projects gnawing on one another."
        imageUri = "https://cards.scryfall.io/normal/front/a/7/a7c0dcac-f3d4-4d6e-9c6e-9295bb1cd47f.jpg"
    }
}
