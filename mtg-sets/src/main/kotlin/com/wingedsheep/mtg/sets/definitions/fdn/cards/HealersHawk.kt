package com.wingedsheep.mtg.sets.definitions.fdn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Healer's Hawk
 * {W}
 * Creature — Bird
 * 1/1
 *
 * Flying
 * Lifelink (Damage dealt by this creature also causes you to gain that much life.)
 */
val HealersHawk = card("Healer's Hawk") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Creature — Bird"
    power = 1
    toughness = 1
    oracleText = "Flying\nLifelink (Damage dealt by this creature also causes you to gain that much life.)"

    keywords(Keyword.FLYING, Keyword.LIFELINK)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "734"
        artist = "Milivoj Ćeran"
        imageUri = "https://cards.scryfall.io/normal/front/0/6/069cfaa5-bba4-4503-b54e-b98fa9f0a0fc.jpg?1775599663"
        flavorText = "The wounded see the glow of its vials long before they see its wings diving out of the clouds."
    }
}
