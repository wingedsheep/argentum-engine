package com.wingedsheep.rulesengine.sets.ecl.cards

import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.Keyword
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype

/**
 * Sun-Dappled Celebrant
 *
 * {4}{W}{W} Creature — Treefolk Cleric 5/6
 * Convoke
 * Vigilance
 */
object SunDappledCelebrant {
    val definition = CardDefinition.creature(
        name = "Sun-Dappled Celebrant",
        manaCost = ManaCost.parse("{4}{W}{W}"),
        subtypes = setOf(Subtype.TREEFOLK, Subtype.CLERIC),
        power = 5,
        toughness = 6,
        keywords = setOf(Keyword.CONVOKE, Keyword.VIGILANCE),
        oracleText = "Convoke\nVigilance",
        metadata = ScryfallMetadata(
            collectorNumber = "37",
            rarity = Rarity.COMMON,
            artist = "Ovidio Cartagena",
            imageUri = "https://cards.scryfall.io/normal/front/d/d/dd5d5d5d-5d5d-5d5d-5d5d-5d5d5d5d5d5d.jpg",
            releaseDate = "2026-01-23"
        )
    )

    val script = cardScript("Sun-Dappled Celebrant") {
        keywords(Keyword.CONVOKE, Keyword.VIGILANCE)
    }
}
