package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.AnyTarget

/**
 * Erratic Explosion
 * {2}{R}
 * Sorcery
 * Choose any target. Reveal cards from the top of your library until you reveal a nonland card.
 * Erratic Explosion deals damage equal to that card's mana value to that permanent or player.
 * Put the revealed cards on the bottom of your library in any order.
 */
val ErraticExplosion = card("Erratic Explosion") {
    manaCost = "{2}{R}"
    typeLine = "Sorcery"
    oracleText = "Choose any target. Reveal cards from the top of your library until you reveal a nonland card. Erratic Explosion deals damage equal to that card's mana value to that permanent or player. Put the revealed cards on the bottom of your library in any order."

    spell {
        target = AnyTarget()
        effect = EffectPatterns.revealUntilNonlandDealDamage(EffectTarget.ContextTarget(0))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "201"
        artist = "Gary Ruddell"
        flavorText = "\"Oops.\"\n—Erratic wizard, last words"
        imageUri = "https://cards.scryfall.io/normal/front/9/f/9f608a7e-5555-4554-a6e7-fe00e0bbe753.jpg"
    }
}
