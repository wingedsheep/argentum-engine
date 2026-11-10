package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Kor Scythemaster
 * {2}{W}
 * Creature — Kor Soldier Ally
 * 3/1
 * This creature has first strike as long as it's attacking.
 */
val KorScythemaster = card("Kor Scythemaster") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Kor Soldier Ally"
    power = 3
    toughness = 1
    oracleText = "This creature has first strike as long as it's attacking."

    staticAbility {
        ability = GrantKeyword(Keyword.FIRST_STRIKE, GroupFilter.source())
        condition = Conditions.SourceIsAttacking
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "23"
        artist = "Ryan Pancoast"
        flavorText = "The kor of Zendikar are known for quiet determination in the face of overwhelming odds."
        imageUri = "https://cards.scryfall.io/normal/front/5/c/5ce587ba-f23a-4548-a5b8-fca54984d4b4.jpg?1783937925"
    }
}
