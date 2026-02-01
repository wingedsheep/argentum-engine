package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Disciple of Malice
 * {1}{B}
 * Creature — Human Cleric
 * 1/2
 * Protection from white
 * Cycling {2}
 */
val DiscipleOfMalice = card("Disciple of Malice") {
    manaCost = "{1}{B}"
    typeLine = "Creature — Human Cleric"
    power = 1
    toughness = 2

    keywordAbility(KeywordAbility.ProtectionFromColor(Color.WHITE))
    keywordAbility(KeywordAbility.cycling("{2}"))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "134"
        artist = "Alex Horley-Orlandelli"
        flavorText = "\"They say 'a little knowledge is a dangerous thing.' I say they don't know the half of it.\""
        imageUri = "https://cards.scryfall.io/normal/front/7/4/74cc7ab0-a5db-4ae9-af9a-89fd5aaaab57.jpg"
    }
}
