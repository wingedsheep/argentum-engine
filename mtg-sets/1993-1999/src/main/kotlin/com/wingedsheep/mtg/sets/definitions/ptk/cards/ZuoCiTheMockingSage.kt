package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlockedBy
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Zuo Ci, the Mocking Sage
 * {1}{G}{G}
 * Legendary Creature — Human Advisor
 */
val ZuoCiTheMockingSage = card("Zuo Ci, the Mocking Sage") {
    manaCost = "{1}{G}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Human Advisor"
    power = 1
    toughness = 2
    oracleText =
        "Hexproof (This creature can't be the target of spells or abilities your opponents control.)\n" +
        "Zuo Ci can't be blocked by creatures with horsemanship."

    keywords(Keyword.HEXPROOF)

    staticAbility {
        ability = CantBeBlockedBy(GameObjectFilter.Creature.withKeyword(Keyword.HORSEMANSHIP))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "165"
        artist = "Wang Yuqun"
        imageUri = "https://cards.scryfall.io/normal/front/e/3/e3425241-efdc-4261-a2bb-58fbf4a9fe8c.jpg"
    }
}
