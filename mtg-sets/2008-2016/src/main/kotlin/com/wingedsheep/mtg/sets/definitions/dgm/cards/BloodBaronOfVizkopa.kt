package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ModifyStats

/**
 * Blood Baron of Vizkopa
 * {3}{W}{B}
 * Creature — Vampire
 * 4/4
 * Lifelink, protection from white and from black
 * As long as you have 30 or more life and an opponent has 10 or less life, this creature gets +6/+6
 * and has flying.
 */
val BloodBaronOfVizkopa = card("Blood Baron of Vizkopa") {
    manaCost = "{3}{W}{B}"
    colorIdentity = "WB"
    typeLine = "Creature — Vampire"
    power = 4
    toughness = 4
    oracleText = "Lifelink, protection from white and from black\n" +
        "As long as you have 30 or more life and an opponent has 10 or less life, this creature gets +6/+6 and has flying."

    keywords(Keyword.LIFELINK)
    keywordAbility(KeywordAbility.protectionFrom(Color.WHITE))
    keywordAbility(KeywordAbility.protectionFrom(Color.BLACK))

    staticAbility {
        condition = Conditions.All(Conditions.LifeAtLeast(30), Conditions.AnOpponentLifeAtMost(10))
        ability = ModifyStats(6, 6, Filters.Self)
    }

    staticAbility {
        condition = Conditions.All(Conditions.LifeAtLeast(30), Conditions.AnOpponentLifeAtMost(10))
        ability = GrantKeyword(Keyword.FLYING, Filters.Self)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "57"
        artist = "Anthony Palumbo"
        imageUri = "https://cards.scryfall.io/normal/front/e/4/e4edad09-bf7b-40e9-ac2a-100da8a43274.jpg?1783940032"
        ruling("2013-04-15", "In a Two-Headed Giant game, Blood Baron of Vizkopa gets +6/+6 and has flying as long as your team has 30 or more life and an opposing team has 10 or less life.")
    }
}
