package com.wingedsheep.rulesengine.sets.ecl.cards

import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.Keyword
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype

/**
 * Virulent Emissary
 *
 * {G} Creature — Elf Assassin 1/1
 * Deathtouch
 * Whenever another creature enters under your control, you gain 1 life.
 */
object VirulentEmissary {
    val definition = CardDefinition.creature(
        name = "Virulent Emissary",
        manaCost = ManaCost.parse("{G}"),
        subtypes = setOf(Subtype.ELF, Subtype.ASSASSIN),
        power = 1,
        toughness = 1,
        keywords = setOf(Keyword.DEATHTOUCH),
        oracleText = "Deathtouch\nWhenever another creature enters under your control, you gain 1 life.",
        metadata = ScryfallMetadata(
            collectorNumber = "202",
            rarity = Rarity.UNCOMMON,
            artist = "Tiffany Turrill",
            imageUri = "https://cards.scryfall.io/normal/front/e/e/eeff5678-9012-3456-ghij-eeff56789012.jpg",
            releaseDate = "2026-01-23"
        )
    )

    val script = cardScript("Virulent Emissary") {
        keywords(Keyword.DEATHTOUCH)

        // TODO: Needs OnOtherCreatureEnters trigger and GainLifeEffect
        // Whenever another creature enters under your control, you gain 1 life
    }
}
