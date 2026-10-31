package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Unchained Berserker
 * {1}{R}
 * Creature — Human Berserker
 * 1/1
 * Protection from white
 * This creature gets +2/+0 as long as it's attacking.
 */
val UnchainedBerserker = card("Unchained Berserker") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Berserker"
    power = 1
    toughness = 1
    oracleText = "Protection from white\nThis creature gets +2/+0 as long as it's attacking."

    keywordAbility(KeywordAbility.protectionFrom(Color.WHITE))

    staticAbility {
        ability = ModifyStats(2, 0, GroupFilter.source())
        condition = Conditions.SourceIsAttacking
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "164"
        artist = "Bram Sels"
        flavorText = "Sathros turned his restraints to revenge."
        imageUri = "https://cards.scryfall.io/normal/front/d/b/db5879c5-4b0a-42fa-a44f-580e0221a32b.jpg?1783932968"
    }
}
