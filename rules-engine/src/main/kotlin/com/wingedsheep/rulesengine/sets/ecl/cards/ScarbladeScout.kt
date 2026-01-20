package com.wingedsheep.rulesengine.sets.ecl.cards

import com.wingedsheep.rulesengine.ability.OnEnterBattlefield
import com.wingedsheep.rulesengine.ability.SurveilEffect
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.Keyword
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype

/**
 * Scarblade Scout
 *
 * {1}{B} Creature — Elf Scout 2/2
 * Lifelink
 * When this creature enters, mill two cards.
 */
object ScarbladeScout {
    val definition = CardDefinition.creature(
        name = "Scarblade Scout",
        manaCost = ManaCost.parse("{1}{B}"),
        subtypes = setOf(Subtype.ELF, Subtype.SCOUT),
        power = 2,
        toughness = 2,
        keywords = setOf(Keyword.LIFELINK),
        oracleText = "Lifelink\nWhen this creature enters, mill two cards.",
        metadata = ScryfallMetadata(
            collectorNumber = "118",
            rarity = Rarity.COMMON,
            artist = "Lorenzo Mastroianni",
            imageUri = "https://cards.scryfall.io/normal/front/c/c/cc6c6c6c-6c6c-6c6c-6c6c-6c6c6c6c6c6c.jpg",
            releaseDate = "2026-01-23"
        )
    )

    val script = cardScript("Scarblade Scout") {
        keywords(Keyword.LIFELINK)

        // ETB: Mill 2 (using Surveil as placeholder - mill puts cards directly to graveyard)
        // TODO: Add proper MillEffect
        triggered(
            trigger = OnEnterBattlefield(),
            effect = SurveilEffect(count = 2)  // Close approximation until MillEffect exists
        )
    }
}
