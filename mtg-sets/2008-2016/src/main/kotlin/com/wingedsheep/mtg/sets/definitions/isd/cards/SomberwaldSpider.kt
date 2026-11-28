package com.wingedsheep.mtg.sets.definitions.isd.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters

/**
 * Somberwald Spider
 * {4}{G}
 * Creature — Spider
 * 2/4
 * Reach (This creature can block creatures with flying.)
 * Morbid — This creature enters with two +1/+1 counters on it if a creature died this turn.
 */
val SomberwaldSpider = card("Somberwald Spider") {
    manaCost = "{4}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Spider"
    oracleText = "Reach (This creature can block creatures with flying.)\nMorbid — This creature enters with two +1/+1 counters on it if a creature died this turn."
    power = 2
    toughness = 4
    keywords(Keyword.REACH)
    replacementEffect(EntersWithCounters(count = 2, selfOnly = true, condition = Conditions.CreatureDiedThisTurn))
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "202"
        artist = "Volkan Baǵa"
        flavorText = "Stensian vampires encourage spiders near their manors so they can rob the webs."
        imageUri = "https://cards.scryfall.io/normal/front/4/3/43003ad7-2f42-4c85-8b00-77cbf3f50a7b.jpg"
    }
}
