package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.eternalize
import com.wingedsheep.sdk.model.Rarity

/**
 * Adorned Pouncer — Hour of Devastation #2 (canonical printing)
 * {1}{W} · Creature — Cat · 1/1
 *
 * Double strike
 * Eternalize {3}{W}{W}
 */
val AdornedPouncer = card("Adorned Pouncer") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Cat"
    power = 1
    toughness = 1
    oracleText = "Double strike\n" +
        "Eternalize {3}{W}{W} ({3}{W}{W}, Exile this card from your graveyard: Create a token that's a copy of it, " +
        "except it's a 4/4 black Zombie Cat with no mana cost. Eternalize only as a sorcery.)"

    keywords(Keyword.DOUBLE_STRIKE)

    eternalize("{3}{W}{W}")

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "2"
        artist = "Slawomir Maniak"
        imageUri = "https://cards.scryfall.io/normal/front/8/6/865e267d-450c-4eeb-b61d-bd0ec5d8534a.jpg?1783936067"
        ruling("2017-07-14", "The token copies exactly what was printed on the original card and nothing else, except the characteristics specifically modified by eternalize. It doesn't copy any information about the object the card was before it was put into your graveyard.")
        ruling("2017-07-14", "The token is a Zombie in addition to its other types and is black instead of its other colors. Its base power and toughness are 4/4. It has no mana cost, and thus its mana value is 0. These are copiable values of the token that other effects may copy.")
    }
}
