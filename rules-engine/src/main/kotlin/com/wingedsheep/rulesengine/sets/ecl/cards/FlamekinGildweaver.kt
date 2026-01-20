package com.wingedsheep.rulesengine.sets.ecl.cards

import com.wingedsheep.rulesengine.ability.CreateTreasureTokensEffect
import com.wingedsheep.rulesengine.ability.OnEnterBattlefield
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.Keyword
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype

/**
 * Flamekin Gildweaver
 *
 * {3}{R} Creature — Elemental Sorcerer 4/3
 * Trample
 * When this creature enters, create a Treasure token.
 */
object FlamekinGildweaver {
    val definition = CardDefinition.creature(
        name = "Flamekin Gildweaver",
        manaCost = ManaCost.parse("{3}{R}"),
        subtypes = setOf(Subtype.ELEMENTAL, Subtype.SORCERER),
        power = 4,
        toughness = 3,
        keywords = setOf(Keyword.TRAMPLE),
        oracleText = "Trample\nWhen this creature enters, create a Treasure token.",
        metadata = ScryfallMetadata(
            collectorNumber = "140",
            rarity = Rarity.COMMON,
            artist = "Aurore Folny",
            imageUri = "https://cards.scryfall.io/normal/front/b/b/bb5b5b5b-5b5b-5b5b-5b5b-5b5b5b5b5b5b.jpg",
            releaseDate = "2026-01-23"
        )
    )

    val script = cardScript("Flamekin Gildweaver") {
        keywords(Keyword.TRAMPLE)

        // ETB: Create a Treasure token
        triggered(
            trigger = OnEnterBattlefield(),
            effect = CreateTreasureTokensEffect()
        )
    }
}
