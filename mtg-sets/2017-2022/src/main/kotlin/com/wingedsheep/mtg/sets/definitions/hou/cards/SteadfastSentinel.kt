package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.eternalize
import com.wingedsheep.sdk.model.Rarity

/**
 * Steadfast Sentinel — Hour of Devastation #24 (canonical printing)
 * {3}{W} · Creature — Human Cleric · 2/3
 *
 * Vigilance
 * Eternalize {4}{W}{W}
 */
val SteadfastSentinel = card("Steadfast Sentinel") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Cleric"
    power = 2
    toughness = 3
    oracleText = "Vigilance\n" +
        "Eternalize {4}{W}{W} ({4}{W}{W}, Exile this card from your graveyard: Create a token that's a copy of it, " +
        "except it's a 4/4 black Zombie Human Cleric with no mana cost. Eternalize only as a sorcery.)"

    keywords(Keyword.VIGILANCE)

    eternalize("{4}{W}{W}")

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "24"
        artist = "Karl Kopinski"
        imageUri = "https://cards.scryfall.io/normal/front/7/0/707a30d2-0fca-4b39-91fb-9b03152705ba.jpg?1783936059"
        ruling("2017-07-14", "For each card with eternalize, a corresponding game play supplement token can be found in some Hour of Devastation booster packs. These supplements are not required to play with cards with eternalize; you can use the same items to represent an eternalized token as you would any other token.")
        ruling("2017-07-14", "If a creature card with eternalize is put into your graveyard during your main phase, you'll have priority immediately afterward. You can activate its eternalize ability before any player can try to exile it, such as with Crook of Condemnation, if it's legal for you to do so.")
        ruling("2017-07-14", "Once you've activated an eternalize ability, the card is immediately exiled. Opponents can't try to stop the ability by exiling the card with an effect such as that of Crook of Condemnation.")
        ruling("2017-07-14", "The token copies exactly what was printed on the original card and nothing else, except the characteristics specifically modified by eternalize. It doesn't copy any information about the object the card was before it was put into your graveyard.")
        ruling("2017-07-14", "The token is a Zombie in addition to its other types and is black instead of its other colors. Its base power and toughness are 4/4. It has no mana cost, and thus its mana value is 0. These are copiable values of the token that other effects may copy.")
    }
}
