package com.wingedsheep.mtg.sets.definitions.arb.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ProtectionScope

/**
 * Sphinx of the Steel Wind
 * {5}{W}{U}{B}
 * Artifact Creature — Sphinx
 * 6 / 6
 *
 * Flying, first strike, vigilance, lifelink, protection from red and from green
 *
 * - The four plain evergreen keywords are the card's keyword set ([Keyword.FLYING],
 *   [Keyword.FIRST_STRIKE], [Keyword.VIGILANCE], [Keyword.LIFELINK]); the builder derives a
 *   `Simple` keyword ability from each.
 * - CR 702.16g: "protection from [quality A] and from [quality B]" is shorthand for two *separate*
 *   protection abilities, so this is two [KeywordAbility.Protection] instances with a
 *   single-colour [ProtectionScope.Color] each — not one [ProtectionScope.Colors] holding a set.
 *   That matters when something removes or counts abilities individually.
 */
val SphinxOfTheSteelWind = card("Sphinx of the Steel Wind") {
    manaCost = "{5}{W}{U}{B}"
    colorIdentity = "WUB"
    typeLine = "Artifact Creature — Sphinx"
    power = 6
    toughness = 6
    oracleText = "Flying, first strike, vigilance, lifelink, protection from red and from green"

    keywords(Keyword.FLYING, Keyword.FIRST_STRIKE, Keyword.VIGILANCE, Keyword.LIFELINK)

    keywordAbility(KeywordAbility.Protection(ProtectionScope.Color(Color.RED)))
    keywordAbility(KeywordAbility.Protection(ProtectionScope.Color(Color.GREEN)))

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "110"
        artist = "Kev Walker"
        flavorText = "No one has properly answered her favorite riddle: \"Why should I spare your life?\""
        imageUri = "https://cards.scryfall.io/normal/front/c/8/c860cf0f-ef68-455f-9c71-a6dd25b51d71.jpg"
    }
}
