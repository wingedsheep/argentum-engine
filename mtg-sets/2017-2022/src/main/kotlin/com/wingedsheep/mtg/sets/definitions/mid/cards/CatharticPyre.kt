package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Cathartic Pyre
 * {1}{R}
 * Instant
 * Choose one —
 * • Cathartic Pyre deals 3 damage to target creature or planeswalker.
 * • Discard up to two cards, then draw that many cards.
 */
val CatharticPyre = card("Cathartic Pyre") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Choose one —\n• Cathartic Pyre deals 3 damage to target creature or planeswalker.\n• Discard up to two cards, then draw that many cards."

    spell {
        modal(chooseCount = 1) {
            mode("Cathartic Pyre deals 3 damage to target creature or planeswalker") {
                val t = target(Targets.CreatureOrPlaneswalker)
                effect = Effects.DealDamage(3, t)
            }
            mode("Discard up to two cards, then draw that many cards") {
                effect = Patterns.Hand.discardUpToThenDraw(2)
            }
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "133"
        artist = "Ryan Yee"
        flavorText = "\"Leave behind the sorrow of this body. Be blessed, being of light.\""
        imageUri = "https://cards.scryfall.io/normal/front/b/0/b045c28a-39f9-4cd9-8f3a-a626b697f409.jpg?1783925603"
    }
}
