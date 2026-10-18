package com.wingedsheep.mtg.sets.definitions.lrw.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GainKeywordsOfCards
import com.wingedsheep.sdk.scripting.KeywordFamily

/**
 * Cairn Wanderer — Lorwyn #105
 *
 * Every graveyard counts, not just its controller's. "Landwalk" and "protection" name families:
 * Cairn Wanderer gains each landwalk and each protection (with its quality) that a creature card
 * in a graveyard has — see [GainKeywordsOfCards].
 */
val CairnWanderer = card("Cairn Wanderer") {
    manaCost = "{4}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Shapeshifter"
    power = 4
    toughness = 4
    oracleText = "Changeling (This card is every creature type.)\nAs long as a creature card with flying is in a graveyard, this creature has flying. The same is true for fear, first strike, double strike, deathtouch, haste, landwalk, lifelink, protection, reach, trample, shroud, and vigilance."

    keywords(Keyword.CHANGELING)

    staticAbility {
        ability = GainKeywordsOfCards(
            keywords = listOf(
                Keyword.FLYING,
                Keyword.FEAR,
                Keyword.FIRST_STRIKE,
                Keyword.DOUBLE_STRIKE,
                Keyword.DEATHTOUCH,
                Keyword.HASTE,
                Keyword.LIFELINK,
                Keyword.REACH,
                Keyword.TRAMPLE,
                Keyword.SHROUD,
                Keyword.VIGILANCE
            ),
            families = setOf(KeywordFamily.LANDWALK, KeywordFamily.PROTECTION)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "105"
        artist = "Nils Hamm"
        imageUri = "https://cards.scryfall.io/normal/front/b/8/b870a7aa-0836-4e3e-8be9-3299bdaded81.jpg?1783942893"
        ruling("2007-10-01", "Cairn Wanderer’s ability looks at all cards in all graveyards. It gains any landwalk abilities and any protection abilities.")
    }
}
