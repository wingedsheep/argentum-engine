package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Akroan Sergeant
 * {2}{R}
 * Creature — Human Soldier
 * 2/2
 * First strike
 * Renown 1
 */
val AkroanSergeant = card("Akroan Sergeant") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Soldier"
    power = 2
    toughness = 2
    oracleText = "First strike (This creature deals combat damage before creatures without first strike.)\nRenown 1 (When this creature deals combat damage to a player, if it isn't renowned, put a +1/+1 counter on it and it becomes renowned.)"

    keywords(Keyword.FIRST_STRIKE)
    keywordAbility(KeywordAbility.renown(1))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "130"
        artist = "Zack Stella"
        imageUri = "https://cards.scryfall.io/normal/front/3/1/31913547-460c-45b3-be23-89f0e3a43325.jpg?1783938333"

        ruling("2015-06-22", "Renown won't trigger when a creature deals combat damage to a planeswalker or another creature. It also won't trigger when a creature deals noncombat damage to a player.")
        ruling("2015-06-22", "If a creature with renown deals combat damage to its controller because that damage was redirected, renown will trigger.")
        ruling("2015-06-22", "If a renown ability triggers, but the creature leaves the battlefield before that ability resolves, the creature doesn't become renowned. Any ability that triggers \"whenever a creature becomes renowned\" won't trigger.")
    }
}
