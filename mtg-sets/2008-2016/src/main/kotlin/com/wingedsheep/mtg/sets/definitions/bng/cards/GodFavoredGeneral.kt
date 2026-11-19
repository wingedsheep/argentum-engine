package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * God-Favored General
 * {1}{W}
 * Creature — Human Soldier
 * 1/1
 *
 * Inspired — Whenever this creature becomes untapped, you may pay {2}{W}. If you do, create two
 * 1/1 white Soldier enchantment creature tokens.
 *
 * Canonical printing: Born of the Gods, the card's earliest real printing.
 */
val GodFavoredGeneral = card("God-Favored General") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Soldier"
    power = 1
    toughness = 1
    oracleText = "Inspired — Whenever this creature becomes untapped, you may pay {2}{W}. If you do, " +
        "create two 1/1 white Soldier enchantment creature tokens."

    triggeredAbility {
        trigger = Triggers.self.becomesUntapped()
        effect = Effects.MayPay(
            cost = ManaCost.parse("{2}{W}"),
            then = Effects.CreateToken(
                power = 1,
                toughness = 1,
                colors = setOf(Color.WHITE),
                creatureTypes = setOf("Soldier"),
                count = 2,
                enchantmentToken = true,
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "14"
        artist = "David Palumbo"
        flavorText = "Someone had to be first to attack, but he was not alone for long."
        imageUri = "https://cards.scryfall.io/normal/front/9/8/984c2db0-5a79-47b8-8a42-5674c6d9d0fd.jpg?1783939581"
    }
}
