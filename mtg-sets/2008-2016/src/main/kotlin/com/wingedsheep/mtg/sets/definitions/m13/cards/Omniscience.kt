package com.wingedsheep.mtg.sets.definitions.m13.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.MayCastWithoutPayingManaCost

/**
 * Omniscience — Magic 2013 #63.
 *
 * The permission is controller-only and otherwise unrestricted: it applies to every spell
 * cast from the controller's hand while Omniscience remains on the battlefield.
 */
val Omniscience = card("Omniscience") {
    manaCost = "{7}{U}{U}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment"
    oracleText = "You may cast spells from your hand without paying their mana costs."

    staticAbility {
        ability = MayCastWithoutPayingManaCost(controllerOnly = true, fromZones = setOf(Zone.HAND))
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "63"
        artist = "Jason Chan"
        flavorText = "\"The things I once imagined would be my greatest achievements were only the first steps toward a future I can only begin to fathom.\"\n—Jace Beleren"
        imageUri = "https://cards.scryfall.io/normal/front/1/0/1088f33e-cb5f-4248-ae8e-280c4e41f291.jpg?1783940505"

        ruling("2018-07-13", "You must follow the normal timing permissions and restrictions of each spell you cast.")
        ruling("2018-07-13", "If you cast a spell \"without paying its mana cost,\" you can't choose to cast it for any alternative costs. You can, however, pay additional costs, such as kicker costs. If the card has any mandatory additional costs, such as that of Tormenting Voice, those must be paid to cast the spell.")
        ruling("2018-07-13", "If a spell has {X} in its mana cost, you must choose 0 as the value of X when casting it without paying its mana cost.")
    }
}
