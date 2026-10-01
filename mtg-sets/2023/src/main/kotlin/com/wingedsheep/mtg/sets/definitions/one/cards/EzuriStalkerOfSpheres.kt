package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Ezuri, Stalker of Spheres
 * {2}{G}{U}
 * Legendary Creature — Phyrexian Elf Warrior
 * 3/3
 *
 * When Ezuri enters, you may pay {3}. If you do, proliferate twice.
 * Whenever you proliferate, draw a card.
 */
val EzuriStalkerOfSpheres = card("Ezuri, Stalker of Spheres") {
    manaCost = "{2}{G}{U}"
    colorIdentity = "GU"
    typeLine = "Legendary Creature — Phyrexian Elf Warrior"
    power = 3
    toughness = 3
    oracleText = "When Ezuri enters, you may pay {3}. If you do, proliferate twice.\n" +
        "Whenever you proliferate, draw a card."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.MayPay(
            cost = ManaCost.parse("{3}"),
            then = Effects.Proliferate() then Effects.Proliferate()
        )
    }

    triggeredAbility {
        trigger = Triggers.you.proliferates()
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "201"
        artist = "Fariba Khamseh"
        flavorText = "\"Ah, my old friends. Have you decided at last to stop clinging to the past and join me in the grand pursuit of perfection?\""
        imageUri = "https://cards.scryfall.io/normal/front/d/3/d38961ce-0257-412f-acec-c5c9886061f8.jpg?1783918001"
    }
}
