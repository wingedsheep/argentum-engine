package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Faithbearer Paladin (Eldritch Moon #25)
 * {4}{W}
 * Creature — Human Knight
 * 3/4
 *
 * Lifelink (Damage dealt by this creature also causes you to gain that much life.)
 */
val FaithbearerPaladin = card("Faithbearer Paladin") {
    manaCost = "{4}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Knight"
    power = 3
    toughness = 4
    oracleText = "Lifelink (Damage dealt by this creature also causes you to gain that much life.)"

    keywords(Keyword.LIFELINK)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "25"
        artist = "Aaron Miller"
        flavorText = "\"I've seen cathars riding to battle without a hint of fear. Perhaps that is its own " +
            "sort of madness.\"\n—Esler, Havengul mercenary"
        imageUri = "https://cards.scryfall.io/normal/front/a/f/af15a774-b97f-4bc0-913d-4d2a047fed8a.jpg"
    }
}
