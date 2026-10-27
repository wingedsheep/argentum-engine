package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.conditions.WasKicked

/**
 * Cragplate Baloth — Zendikar Rising #183
 * {5}{G}{G} · Creature — Beast · 6/6
 *
 * Kicker {2}{G}
 * This spell can't be countered.
 * Hexproof, haste
 * If this creature was kicked, it enters with four +1/+1 counters on it.
 *
 * The kicked counters are a self-only [EntersWithCounters] replacement gated on [WasKicked], so
 * the Baloth enters already a 10/10 rather than receiving the counters afterwards.
 */
val CragplateBaloth = card("Cragplate Baloth") {
    manaCost = "{5}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Beast"
    oracleText = "Kicker {2}{G}\n" +
        "This spell can't be countered.\n" +
        "Hexproof, haste\n" +
        "If this creature was kicked, it enters with four +1/+1 counters on it."
    power = 6
    toughness = 6

    keywordAbility(KeywordAbility.kicker("{2}{G}"))
    keywords(Keyword.HEXPROOF, Keyword.HASTE)
    cantBeCountered = true

    replacementEffect(
        EntersWithCounters(
            count = 4,
            selfOnly = true,
            condition = WasKicked,
        )
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "183"
        artist = "Jesper Ejsing"
        imageUri = "https://cards.scryfall.io/normal/front/a/b/ab62382d-2dc9-4a60-b031-c845ebad0357.jpg"
    }
}
