package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Firefiend Elemental
 * {3}{R}
 * Creature — Elemental
 * 3/2
 * Haste
 * Renown 1
 */
val FirefiendElemental = card("Firefiend Elemental") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Elemental"
    power = 3
    toughness = 2
    oracleText = "Haste (This creature can attack and {T} as soon as it comes under your control.)\nRenown 1 (When this creature deals combat damage to a player, if it isn't renowned, put a +1/+1 counter on it and it becomes renowned.)"

    keywords(Keyword.HASTE)
    keywordAbility(KeywordAbility.renown(1))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "146"
        artist = "Torstein Nordstrand"
        imageUri = "https://cards.scryfall.io/normal/front/5/5/55051412-8749-4b65-ac76-c20ef57fd2e3.jpg?1783938330"

        ruling("2015-06-22", "Renown won't trigger when a creature deals combat damage to a planeswalker or another creature. It also won't trigger when a creature deals noncombat damage to a player.")
        ruling("2015-06-22", "If a creature with renown deals combat damage to its controller because that damage was redirected, renown will trigger.")
        ruling("2015-06-22", "If a renown ability triggers, but the creature leaves the battlefield before that ability resolves, the creature doesn't become renowned. Any ability that triggers \"whenever a creature becomes renowned\" won't trigger.")
    }
}
