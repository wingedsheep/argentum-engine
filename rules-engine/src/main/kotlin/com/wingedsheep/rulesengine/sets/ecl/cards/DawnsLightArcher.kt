package com.wingedsheep.rulesengine.sets.ecl.cards

import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.Keyword
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype

/**
 * Dawn's Light Archer
 *
 * {2}{G} Creature — Elf Archer 4/2
 * Flash
 * Reach
 */
object DawnsLightArcher {
    val definition = CardDefinition.creature(
        name = "Dawn's Light Archer",
        manaCost = ManaCost.parse("{2}{G}"),
        subtypes = setOf(Subtype.ELF, Subtype.of("Archer")),
        power = 4,
        toughness = 2,
        keywords = setOf(Keyword.FLASH, Keyword.REACH),
        oracleText = "Flash\nReach",
        metadata = ScryfallMetadata(
            collectorNumber = "174",
            rarity = Rarity.COMMON,
            artist = "Scott Gustafson",
            imageUri = "https://cards.scryfall.io/normal/front/c/c/ccddeeff-3456-7890-cdef-ccddeeff3456.jpg",
            releaseDate = "2026-01-23"
        )
    )

    val script = cardScript("Dawn's Light Archer") {
        keywords(Keyword.FLASH, Keyword.REACH)
    }
}
