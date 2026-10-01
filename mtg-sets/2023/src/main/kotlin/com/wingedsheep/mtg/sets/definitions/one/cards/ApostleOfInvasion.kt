package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Apostle of Invasion
 * {4}{W}{W}
 * Creature — Phyrexian Angel
 * 4/4
 * Flying
 * Corrupted — As long as an opponent has three or more poison counters, this creature has
 * double strike.
 */
val ApostleOfInvasion = card("Apostle of Invasion") {
    manaCost = "{4}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Phyrexian Angel"
    oracleText = "Flying\nCorrupted — As long as an opponent has three or more poison counters, this creature has double strike."
    power = 4
    toughness = 4

    keywords(Keyword.FLYING)

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.DOUBLE_STRIKE, GroupFilter.source()),
            condition = Conditions.Corrupted
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "3"
        artist = "Marcela Bolívar"
        flavorText = "\"Be not afraid of the holy chorus. Join in rapturous harmony.\""
        imageUri = "https://cards.scryfall.io/normal/front/8/a/8a973487-5def-4771-bb77-5748cbd2f469.jpg?1783918085"
    }
}
