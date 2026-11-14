package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Goblin Glory Chaser
 * {R}
 * Creature — Goblin Warrior
 * 1/1
 *
 * Renown 1 (When this creature deals combat damage to a player, if it isn't renowned, put a +1/+1 counter on it and it becomes renowned.)
 * As long as this creature is renowned, it has menace. (It can't be blocked except by two or more creatures.)
 */
val GoblinGloryChaser = card("Goblin Glory Chaser") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Creature — Goblin Warrior"
    oracleText = "Renown 1 (When this creature deals combat damage to a player, if it isn't renowned, put a +1/+1 counter on it and it becomes renowned.)\n" +
        "As long as this creature is renowned, it has menace. (It can't be blocked except by two or more creatures.)"
    power = 1
    toughness = 1

    keywordAbility(KeywordAbility.renown(1))

    staticAbility {
        ability = GrantKeyword(Keyword.MENACE, GroupFilter.source())
        condition = Conditions.SourceIsRenowned
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "150"
        artist = "Greg Staples"
        imageUri = "https://cards.scryfall.io/normal/front/e/2/e2779415-9e0e-4c61-b590-e15ac089cf30.jpg?1783938329"
    }
}
