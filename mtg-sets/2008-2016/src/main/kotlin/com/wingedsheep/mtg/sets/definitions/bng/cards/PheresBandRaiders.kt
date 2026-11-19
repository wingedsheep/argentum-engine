package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Pheres-Band Raiders
 * {5}{G}
 * Creature — Centaur Warrior
 * 5/5
 *
 * Inspired — Whenever this creature becomes untapped, you may pay {2}{G}. If you do, create a 3/3
 * green Centaur enchantment creature token.
 *
 * Canonical printing: Born of the Gods, the card's earliest real printing.
 */
val PheresBandRaiders = card("Pheres-Band Raiders") {
    manaCost = "{5}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Centaur Warrior"
    power = 5
    toughness = 5
    oracleText = "Inspired — Whenever this creature becomes untapped, you may pay {2}{G}. If you do, " +
        "create a 3/3 green Centaur enchantment creature token."

    triggeredAbility {
        trigger = Triggers.self.becomesUntapped()
        effect = Effects.MayPay(
            cost = ManaCost.parse("{2}{G}"),
            then = Effects.CreateToken(
                power = 3,
                toughness = 3,
                colors = setOf(Color.GREEN),
                creatureTypes = setOf("Centaur"),
                imageUri = "https://cards.scryfall.io/normal/front/9/8/985be507-6125-4db2-b99f-8b61149ffeeb.jpg?1783939515",
                enchantmentToken = true,
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "133"
        artist = "Ryan Barger"
        flavorText = "\"Who can deny the call to battle when it sings in our blood?\""
        imageUri = "https://cards.scryfall.io/normal/front/8/0/80856020-f569-4d73-8c44-7b48535a69e8.jpg?1783939532"
    }
}
