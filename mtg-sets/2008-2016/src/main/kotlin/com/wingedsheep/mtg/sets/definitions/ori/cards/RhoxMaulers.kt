package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Rhox Maulers
 * {4}{G}
 * Creature — Rhino Soldier
 * 4/4
 * Trample
 * Renown 2
 */
val RhoxMaulers = card("Rhox Maulers") {
    manaCost = "{4}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Rhino Soldier"
    power = 4
    toughness = 4
    oracleText = "Trample (This creature can deal excess combat damage to the player or planeswalker it's attacking.)\nRenown 2 (When this creature deals combat damage to a player, if it isn't renowned, put two +1/+1 counters on it and it becomes renowned.)"

    keywords(Keyword.TRAMPLE)
    keywordAbility(KeywordAbility.renown(2))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "196"
        artist = "Zoltan Boros"
        imageUri = "https://cards.scryfall.io/normal/front/6/4/64c3c972-82f6-46ea-8f9f-090c65c22e44.jpg?1783938318"

        ruling("2015-06-22", "Renown won't trigger when a creature deals combat damage to a planeswalker or another creature. It also won't trigger when a creature deals noncombat damage to a player.")
        ruling("2015-06-22", "If a creature with renown deals combat damage to its controller because that damage was redirected, renown will trigger.")
        ruling("2015-06-22", "If a renown ability triggers, but the creature leaves the battlefield before that ability resolves, the creature doesn't become renowned. Any ability that triggers \"whenever a creature becomes renowned\" won't trigger.")
    }
}
