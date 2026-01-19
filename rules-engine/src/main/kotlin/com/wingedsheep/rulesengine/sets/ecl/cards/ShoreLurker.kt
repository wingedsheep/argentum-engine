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
 * Shore Lurker
 *
 * {3}{W} Creature — Merfolk Scout 3/3
 * Flying
 * When this creature enters, surveil 1.
 */
object ShoreLurker {
    val definition = CardDefinition.creature(
        name = "Shore Lurker",
        manaCost = ManaCost.parse("{3}{W}"),
        subtypes = setOf(Subtype.MERFOLK, Subtype.SCOUT),
        power = 3,
        toughness = 3,
        keywords = setOf(Keyword.FLYING),
        oracleText = "Flying\nWhen this creature enters, surveil 1.",
        metadata = ScryfallMetadata(
            collectorNumber = "34",
            rarity = Rarity.COMMON,
            artist = "Kev Fang",
            imageUri = "https://cards.scryfall.io/normal/front/a/a/aa93f4e5-f9e1-4f3f-8c78-63c90e03f6e3.jpg",
            releaseDate = "2026-01-23"
        )
    )

    val script = cardScript("Shore Lurker") {
        keywords(Keyword.FLYING)

        // ETB: Surveil 1
        triggered(
            trigger = OnEnterBattlefield(),
            effect = SurveilEffect(count = 1)
        )
    }
}
