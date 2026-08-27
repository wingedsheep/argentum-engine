package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * War Oracle
 * {2}{W}{W}
 * Creature — Human Cleric
 * 3/3
 * Lifelink
 * Renown 1
 */
val WarOracle = card("War Oracle") {
    manaCost = "{2}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Cleric"
    power = 3
    toughness = 3
    oracleText = "Lifelink (Damage dealt by this creature also causes you to gain that much life.)\nRenown 1 (When this creature deals combat damage to a player, if it isn't renowned, put a +1/+1 counter on it and it becomes renowned.)"

    keywords(Keyword.LIFELINK)
    keywordAbility(KeywordAbility.renown(1))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "41"
        artist = "Steve Prescott"
        flavorText = "\"When you are felled by my mace, you shall know it was divine fate.\""
        imageUri = "https://cards.scryfall.io/normal/front/3/d/3d8827bf-11c3-4f78-b7aa-ae953442c709.jpg?1783938356"

        ruling("2015-06-22", "Renown won't trigger when a creature deals combat damage to a planeswalker or another creature. It also won't trigger when a creature deals noncombat damage to a player.")
        ruling("2015-06-22", "If a creature with renown deals combat damage to its controller because that damage was redirected, renown will trigger.")
        ruling("2015-06-22", "If a renown ability triggers, but the creature leaves the battlefield before that ability resolves, the creature doesn't become renowned. Any ability that triggers \"whenever a creature becomes renowned\" won't trigger.")
    }
}
