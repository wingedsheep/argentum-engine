package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Thrun, Breaker of Silence
 * {3}{G}{G}
 * Legendary Creature — Troll Shaman
 * 5/5
 * This spell can't be countered.
 * Trample
 * Thrun can't be the target of nongreen spells your opponents control or abilities from nongreen
 * sources your opponents control.
 * During your turn, Thrun has indestructible.
 *
 * The targeting clause is CR 702.11d's definition of "hexproof from nongreen" word for word, so it
 * is authored as that quality — colorless sources are nongreen (CR 105.2c) and are stopped too, while
 * a green-white spell is green and gets through.
 */
val ThrunBreakerOfSilence = card("Thrun, Breaker of Silence") {
    manaCost = "{3}{G}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Troll Shaman"
    power = 5
    toughness = 5
    oracleText = "This spell can't be countered.\n" +
        "Trample\n" +
        "Thrun can't be the target of nongreen spells your opponents control or abilities from " +
        "nongreen sources your opponents control.\n" +
        "During your turn, Thrun has indestructible."

    cantBeCountered = true

    keywords(Keyword.TRAMPLE)

    keywordAbility(KeywordAbility.hexproofFromNon(Color.GREEN))

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.INDESTRUCTIBLE, GroupFilter.source()),
            condition = Conditions.IsYourTurn
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "186"
        artist = "Simon Dominic"
        imageUri = "https://cards.scryfall.io/normal/front/6/d/6d9f51dd-0393-4b3c-bea5-8f74634ab0e5.jpg?1783918008"
    }
}
