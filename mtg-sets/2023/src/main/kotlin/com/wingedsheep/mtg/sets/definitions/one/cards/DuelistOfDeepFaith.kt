package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Duelist of Deep Faith
 * {1}{W}
 * Creature — Phyrexian Soldier
 * 2/2
 * Toxic 1
 * During your turn, this creature has first strike.
 */
val DuelistOfDeepFaith = card("Duelist of Deep Faith") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Phyrexian Soldier"
    power = 2
    toughness = 2
    oracleText = "Toxic 1 (Players dealt combat damage by this creature also get a poison counter.)\n" +
        "During your turn, this creature has first strike."

    keywordAbility(KeywordAbility.toxic(1))

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.FIRST_STRIKE, Filters.Self),
            condition = Conditions.IsYourTurn,
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "9"
        artist = "Marcela Bolívar"
        flavorText = "\"There is no hate behind our blades. We kill out of love, and the desire to share Norn's blessings with all.\""
        imageUri = "https://cards.scryfall.io/normal/front/5/6/56444440-a9e6-4583-a289-9f0571a98093.jpg?1783918083"
    }
}
