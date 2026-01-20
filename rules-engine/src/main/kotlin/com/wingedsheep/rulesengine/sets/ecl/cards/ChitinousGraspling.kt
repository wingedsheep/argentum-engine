package com.wingedsheep.rulesengine.sets.ecl.cards

import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.Keyword
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype

/**
 * Chitinous Graspling
 *
 * {3}{G/U} Creature — Shapeshifter 3/4
 * Changeling
 * Reach
 */
object ChitinousGraspling {
    val definition = CardDefinition.creature(
        name = "Chitinous Graspling",
        manaCost = ManaCost.parse("{3}{G/U}"),
        subtypes = setOf(Subtype.SHAPESHIFTER),
        power = 3,
        toughness = 4,
        keywords = setOf(Keyword.CHANGELING, Keyword.REACH),
        oracleText = "Changeling\nReach",
        metadata = ScryfallMetadata(
            collectorNumber = "211",
            rarity = Rarity.COMMON,
            artist = "Richard Kane Ferguson",
            imageUri = "https://cards.scryfall.io/normal/front/f/f/ffgg7890-1234-5678-ijkl-ffgg78901234.jpg",
            releaseDate = "2026-01-23"
        )
    )

    val script = cardScript("Chitinous Graspling") {
        keywords(Keyword.CHANGELING, Keyword.REACH)
    }
}
