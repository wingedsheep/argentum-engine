package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Pharika's Disciple
 * {3}{G}
 * Creature — Centaur Warrior
 * 2/3
 * Deathtouch
 * Renown 1
 */
val PharikasDisciple = card("Pharika's Disciple") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Centaur Warrior"
    power = 2
    toughness = 3
    oracleText = "Deathtouch (Any amount of damage this deals to a creature is enough to destroy it.)\nRenown 1 (When this creature deals combat damage to a player, if it isn't renowned, put a +1/+1 counter on it and it becomes renowned.)"

    keywords(Keyword.DEATHTOUCH)
    keywordAbility(KeywordAbility.renown(1))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "194"
        artist = "Karl Kopinski"
        imageUri = "https://cards.scryfall.io/normal/front/f/0/f0b3d8f7-6a41-49ba-b111-d34a345394c0.jpg?1783938319"

        ruling("2015-06-22", "Renown won't trigger when a creature deals combat damage to a planeswalker or another creature. It also won't trigger when a creature deals noncombat damage to a player.")
        ruling("2015-06-22", "If a creature with renown deals combat damage to its controller because that damage was redirected, renown will trigger.")
        ruling("2015-06-22", "If a renown ability triggers, but the creature leaves the battlefield before that ability resolves, the creature doesn't become renowned. Any ability that triggers \"whenever a creature becomes renowned\" won't trigger.")
    }
}
