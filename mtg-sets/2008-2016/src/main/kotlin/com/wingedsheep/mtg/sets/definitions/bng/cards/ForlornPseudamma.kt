package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Forlorn Pseudamma
 * {3}{B}
 * Creature — Zombie
 * 2/1
 *
 * Intimidate
 * Inspired — Whenever this creature becomes untapped, you may pay {2}{B}. If you do, create a 2/2
 * black Zombie enchantment creature token.
 *
 * Canonical printing: Born of the Gods, the card's earliest real printing.
 */
val ForlornPseudamma = card("Forlorn Pseudamma") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie"
    power = 2
    toughness = 1
    oracleText = "Intimidate (This creature can't be blocked except by artifact creatures and/or creatures that share a color with it.)\n" +
        "Inspired — Whenever this creature becomes untapped, you may pay {2}{B}. If you do, " +
        "create a 2/2 black Zombie enchantment creature token."

    keywords(Keyword.INTIMIDATE)

    triggeredAbility {
        trigger = Triggers.self.becomesUntapped()
        effect = Effects.MayPay(
            cost = ManaCost.parse("{2}{B}"),
            then = Effects.CreateToken(
                power = 2,
                toughness = 2,
                colors = setOf(Color.BLACK),
                creatureTypes = setOf("Zombie"),
                enchantmentToken = true,
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "71"
        artist = "Winona Nelson"
        flavorText = "\"More children taken. This is an evil we will track without mercy.\"\n—Anthousa of Setessa"
        imageUri = "https://cards.scryfall.io/normal/front/1/c/1c236a33-fb20-41f4-8946-beb65d1cf684.jpg?1783939558"
    }
}
