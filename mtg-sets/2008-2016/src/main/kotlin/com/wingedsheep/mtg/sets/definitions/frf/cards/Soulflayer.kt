package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GainKeywordsOfCards
import com.wingedsheep.sdk.scripting.KeywordCardPool
import com.wingedsheep.sdk.scripting.KeywordFamily

/**
 * Soulflayer — Fate Reforged #84
 * {4}{B}{B}
 * Creature — Demon
 * 4/4
 *
 * The delve payment is recorded on the spell and carried onto the permanent
 * ([KeywordCardPool.CREATURE_CARDS_EXILED_WITH_DELVE]); each listed keyword printed on a delved
 * creature card is granted. "Hexproof" is a family so a delved card's hexproof from a quality
 * grants that variant (the printed ruling).
 */
val Soulflayer = card("Soulflayer") {
    manaCost = "{4}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Demon"
    power = 4
    toughness = 4
    oracleText = "Delve (Each card you exile from your graveyard while casting this spell pays for {1}.)\n" +
        "If a creature card with flying was exiled with this creature's delve ability, this creature has flying. " +
        "The same is true for first strike, double strike, deathtouch, haste, hexproof, indestructible, lifelink, reach, trample, and vigilance."

    keywords(Keyword.DELVE)

    staticAbility {
        ability = GainKeywordsOfCards(
            keywords = listOf(
                Keyword.FLYING,
                Keyword.FIRST_STRIKE,
                Keyword.DOUBLE_STRIKE,
                Keyword.DEATHTOUCH,
                Keyword.HASTE,
                Keyword.INDESTRUCTIBLE,
                Keyword.LIFELINK,
                Keyword.REACH,
                Keyword.TRAMPLE,
                Keyword.VIGILANCE
            ),
            families = setOf(KeywordFamily.HEXPROOF),
            cards = KeywordCardPool.CREATURE_CARDS_EXILED_WITH_DELVE
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "84"
        artist = "Seb McKinnon"
        imageUri = "https://cards.scryfall.io/normal/front/5/0/5084c8ff-1296-4d8e-bd06-93b1a3401661.jpg?1783938694"
        ruling("2020-01-24", "If one of those creature cards has one or more variants of the listed keywords (for example, hexproof from white), this creature gains those specific variants.")
        ruling("2014-11-24", "Creature cards that have abilities that grant the listed keyword abilities to themselves won't count. For example, exiling a Battle Brawler (a creature with \"As long as you control a red or white permanent, Battle Brawler gets +1/+0 and has first strike\") with Soulflayer's delve ability won't cause Soulflayer to have first strike, even if you control a red or white permanent.")
        ruling("2014-11-24", "You can't exile more cards from your graveyard than you'd need to pay Soulflayer's generic mana requirement. In most situations, this means you can't exile more than four cards, even if you want to exile more to give it extra abilities.")
    }
}
