package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GrantKeyword

/**
 * Leech Fanatic — Strixhaven: School of Mages #75 (canonical printing)
 * {1}{B} · Creature — Human Warlock · 2/2
 *
 * During your turn, this creature has lifelink.
 *
 * The Tonberry shape: a [GrantKeyword] on [Filters.Self] gated by [Conditions.IsYourTurn].
 */
val LeechFanatic = card("Leech Fanatic") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Warlock"
    power = 2
    toughness = 2
    oracleText = "During your turn, this creature has lifelink."

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.LIFELINK, Filters.Self),
            condition = Conditions.IsYourTurn,
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "75"
        artist = "Sean Sevestre"
        flavorText = "\"Everyone thinks that leeches maintain a strictly sanguivorous diet, but in fact, most freshwater leeches are carnivores. Wait, where are you going?\""
        imageUri = "https://cards.scryfall.io/normal/front/3/0/307f9fe7-241b-4eb6-a059-be5384b4a1b6.jpg?1783927366"
    }
}
