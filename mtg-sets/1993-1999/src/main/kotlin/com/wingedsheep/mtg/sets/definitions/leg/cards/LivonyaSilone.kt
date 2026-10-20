package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Livonya Silone
 * {2}{R}{R}{G}{G}
 * Legendary Creature — Human Warrior
 * 4/4
 *
 * First strike; legendary landwalk (This creature can't be blocked as long as defending player
 * controls a legendary land.)
 *
 * Legendary landwalk is [Keyword.LEGENDARY_LANDWALK], a member of the CR 702.14 landwalk family, so
 * landwalk removal (Hammerheim) and suppression (`SuppressLandwalkForGroup`) reach it too.
 */
val LivonyaSilone = card("Livonya Silone") {
    manaCost = "{2}{R}{R}{G}{G}"
    colorIdentity = "RG"
    typeLine = "Legendary Creature — Human Warrior"
    power = 4
    toughness = 4
    oracleText = "First strike; legendary landwalk (This creature can't be blocked as long as defending " +
        "player controls a legendary land.)"

    keywords(Keyword.FIRST_STRIKE, Keyword.LEGENDARY_LANDWALK)

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "242"
        artist = "Richard Kane Ferguson"
        imageUri = "https://cards.scryfall.io/normal/front/b/9/b9211949-66a5-4039-ac6d-3e42b008b58e.jpg?1783948036"
        ruling(
            "2009-10-01",
            "\"Legendary landwalk\" means \"This creature is can't be blocked as long as defending player " +
                "controls a legendary land.\""
        )
    }
}
