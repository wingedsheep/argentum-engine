package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Topan Freeblade
 * {1}{W}
 * Creature — Human Soldier
 * 2/2
 * Vigilance
 * Renown 1
 */
val TopanFreeblade = card("Topan Freeblade") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Soldier"
    power = 2
    toughness = 2
    oracleText = "Vigilance (Attacking doesn't cause this creature to tap.)\nRenown 1 (When this creature deals combat damage to a player, if it isn't renowned, put a +1/+1 counter on it and it becomes renowned.)"

    keywords(Keyword.VIGILANCE)
    keywordAbility(KeywordAbility.renown(1))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "36"
        artist = "Johannes Voss"
        flavorText = "\"My scars are my sigils. I will wear them with pride long after you're gone.\""
        imageUri = "https://cards.scryfall.io/normal/front/b/9/b9ef727b-3e67-46d2-80f9-b1d483977b05.jpg?1783938358"

        ruling("2015-06-22", "Renown won't trigger when a creature deals combat damage to a planeswalker or another creature. It also won't trigger when a creature deals noncombat damage to a player.")
        ruling("2015-06-22", "If a creature with renown deals combat damage to its controller because that damage was redirected, renown will trigger.")
        ruling("2015-06-22", "If a renown ability triggers, but the creature leaves the battlefield before that ability resolves, the creature doesn't become renowned. Any ability that triggers \"whenever a creature becomes renowned\" won't trigger.")
    }
}
