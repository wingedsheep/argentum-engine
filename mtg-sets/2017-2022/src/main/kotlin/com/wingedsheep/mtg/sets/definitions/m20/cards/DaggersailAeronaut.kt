package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GrantKeyword

/**
 * Daggersail Aeronaut — Core Set 2020 #133
 * {3}{R} · Creature — Goblin · Common
 * 3/2
 *
 * During your turn, this creature has flying.
 *
 * A self-scoped [GrantKeyword] gated by [Conditions.IsYourTurn] — same shape as Blood Burglar.
 */
val DaggersailAeronaut = card("Daggersail Aeronaut") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Goblin"
    power = 3
    toughness = 2
    oracleText = "During your turn, this creature has flying."

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.FLYING, Filters.Self),
            condition = Conditions.IsYourTurn,
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "133"
        artist = "Zoltan Boros"
        flavorText = "\"Coming down is the best part!\""
        imageUri = "https://cards.scryfall.io/normal/front/5/a/5a46b090-8b03-4498-840e-cf130510892c.jpg?1783932980"
    }
}
