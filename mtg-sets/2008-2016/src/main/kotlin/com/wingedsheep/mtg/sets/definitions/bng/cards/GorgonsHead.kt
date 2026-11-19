package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword

/**
 * Gorgon's Head
 * {1}
 * Artifact — Equipment
 *
 * Equipped creature has deathtouch.
 * Equip {2}
 *
 * Canonical printing: Born of the Gods, the card's earliest real printing.
 */
val GorgonsHead = card("Gorgon's Head") {
    manaCost = "{1}"
    colorIdentity = ""
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature has deathtouch.\nEquip {2}"

    staticAbility {
        ability = GrantKeyword(Keyword.DEATHTOUCH)
    }

    equipAbility("{2}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "158"
        artist = "Michael C. Hayes"
        flavorText = "\"You slew the gorgon? Show me.\"\n—King Igalus, last words"
        imageUri = "https://cards.scryfall.io/normal/front/d/7/d745dd92-9f7a-44a4-a27f-2ff17753db1c.jpg?1783939522"
    }
}
