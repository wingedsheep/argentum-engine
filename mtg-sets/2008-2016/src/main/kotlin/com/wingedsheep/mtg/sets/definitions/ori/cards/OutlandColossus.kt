package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlockedByMoreThan
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Outland Colossus
 * {3}{G}{G}
 * Creature — Giant
 * 6/6
 * Renown 6
 * This creature can't be blocked by more than one creature.
 */
val OutlandColossus = card("Outland Colossus") {
    manaCost = "{3}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Giant"
    power = 6
    toughness = 6
    oracleText = "Renown 6 (When this creature deals combat damage to a player, if it isn't renowned, put six +1/+1 counters on it and it becomes renowned.)\nThis creature can't be blocked by more than one creature."

    keywordAbility(KeywordAbility.renown(6))

    staticAbility {
        ability = CantBeBlockedByMoreThan(maxBlockers = 1)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "193"
        artist = "Ryan Pancoast"
        imageUri = "https://cards.scryfall.io/normal/front/1/c/1caad298-52cb-46f1-8212-fe657ab80159.jpg?1783938318"

        ruling("2015-06-22", "Renown won't trigger when a creature deals combat damage to a planeswalker or another creature. It also won't trigger when a creature deals noncombat damage to a player.")
        ruling("2015-06-22", "If a creature with renown deals combat damage to its controller because that damage was redirected, renown will trigger.")
        ruling("2015-06-22", "If a renown ability triggers, but the creature leaves the battlefield before that ability resolves, the creature doesn't become renowned. Any ability that triggers \"whenever a creature becomes renowned\" won't trigger.")
    }
}
