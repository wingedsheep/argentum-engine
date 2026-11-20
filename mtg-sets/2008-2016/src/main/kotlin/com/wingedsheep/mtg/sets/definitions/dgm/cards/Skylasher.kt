package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Skylasher
 * {1}{G}
 * Creature — Insect
 * 2/2
 * Flash
 * This spell can't be countered.
 * Reach, protection from blue
 */
val Skylasher = card("Skylasher") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Insect"
    power = 2
    toughness = 2
    oracleText = "Flash\nThis spell can't be countered.\nReach, protection from blue"

    keywords(Keyword.FLASH, Keyword.REACH)
    keywordAbility(KeywordAbility.protectionFrom(Color.BLUE))

    cantBeCountered = true

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "49"
        artist = "Dan Murayama Scott"
        flavorText = "\"It preys on those who would spy on our lands, so we never prey on it.\"\n—Kroshkar, Gruul shaman"
        imageUri = "https://cards.scryfall.io/normal/front/4/f/4f4c2069-deb1-4e56-8069-170c4f495944.jpg?1783940034"
    }
}
