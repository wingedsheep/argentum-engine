package com.wingedsheep.rulesengine.sets.ecl.cards

import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.Keyword
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype

/**
 * Wildvine Pummeler
 *
 * {6}{G} Creature — Giant Berserker 6/5
 * Vivid — This spell costs {X} less to cast, where X is the number of colors among permanents you control.
 * Reach
 * Trample
 */
object WildvinePummeler {
    val definition = CardDefinition.creature(
        name = "Wildvine Pummeler",
        manaCost = ManaCost.parse("{6}{G}"),
        subtypes = setOf(Subtype.GIANT, Subtype.BERSERKER),
        power = 6,
        toughness = 5,
        keywords = setOf(Keyword.REACH, Keyword.TRAMPLE),
        oracleText = "Vivid — This spell costs {X} less to cast, where X is the number of colors among permanents you control.\nReach\nTrample",
        metadata = ScryfallMetadata(
            collectorNumber = "203",
            rarity = Rarity.COMMON,
            artist = "Kev Walker",
            imageUri = "https://cards.scryfall.io/normal/front/h/h/hhii0123-4567-8901-mnop-hhii01234567.jpg",
            releaseDate = "2026-01-23"
        )
    )

    val script = cardScript("Wildvine Pummeler") {
        keywords(Keyword.REACH, Keyword.TRAMPLE)

        // TODO: Vivid cost reduction needs spell cost modification infrastructure
    }
}
