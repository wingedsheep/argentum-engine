package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Knight of the Pilgrim's Road
 * {2}{W}
 * Creature — Human Knight
 * 3/2
 * Renown 1
 */
val KnightOfThePilgrimsRoad = card("Knight of the Pilgrim's Road") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Knight"
    power = 3
    toughness = 2
    oracleText = "Renown 1 (When this creature deals combat damage to a player, if it isn't renowned, put a +1/+1 counter on it and it becomes renowned.)"

    keywordAbility(KeywordAbility.renown(1))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "20"
        artist = "David Gaillet"
        flavorText = "\"To be a knight, Gideon, is to be the shield for the meek against the cruel.\""
        imageUri = "https://cards.scryfall.io/normal/front/b/9/b9de5f39-b07a-4272-8992-ed971132c9c4.jpg?1783938360"

        ruling("2015-06-22", "Renown won't trigger when a creature deals combat damage to a planeswalker or another creature. It also won't trigger when a creature deals noncombat damage to a player.")
        ruling("2015-06-22", "If a creature with renown deals combat damage to its controller because that damage was redirected, renown will trigger.")
        ruling("2015-06-22", "If a renown ability triggers, but the creature leaves the battlefield before that ability resolves, the creature doesn't become renowned. Any ability that triggers \"whenever a creature becomes renowned\" won't trigger.")
    }
}
