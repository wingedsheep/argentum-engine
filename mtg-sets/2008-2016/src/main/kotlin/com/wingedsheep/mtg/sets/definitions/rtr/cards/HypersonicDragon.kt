package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantFlashToSpellType

/**
 * Hypersonic Dragon
 * {3}{U}{R}
 * Creature — Dragon
 * 4/4
 * Flying, haste
 * You may cast sorcery spells as though they had flash.
 *
 * The flash permission is a [GrantFlashToSpellType] static over sorcery spells, controller-only
 * ("you may cast") — the sorcery half of Whirlwing Stormbrood's grant.
 */
val HypersonicDragon = card("Hypersonic Dragon") {
    manaCost = "{3}{U}{R}"
    colorIdentity = "UR"
    typeLine = "Creature — Dragon"
    power = 4
    toughness = 4
    oracleText = "Flying, haste\n" +
        "You may cast sorcery spells as though they had flash. " +
        "(You may cast them any time you could cast an instant.)"

    keywords(Keyword.FLYING, Keyword.HASTE)

    staticAbility {
        ability = GrantFlashToSpellType(
            filter = GameObjectFilter.Sorcery,
            controllerOnly = true
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "170"
        artist = "Dan Murayama Scott"
        flavorText = "\"Even this primitive specimen of my kind is impressive.\"\n—Niv-Mizzet"
        imageUri = "https://cards.scryfall.io/normal/front/f/0/f024b19a-923a-4313-be06-e743d3fbab46.jpg?1783940338"
    }
}
