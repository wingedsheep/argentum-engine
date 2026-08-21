package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Gargadon — Modern Horizons 2 #128
 * {5}{R}{R} · Creature — Beast · 7 / 5
 *
 * Trample
 * Suspend 4—{1}{R} (Rather than cast this card from your hand, you may pay {1}{R} and exile it with four time counters on it. At the beginning of your upkeep, remove a time counter. When the last is removed, you may cast it without paying its mana cost. It has haste.)
 *
 * A vanilla body plus the two printed keywords, so nothing is authored beyond them.
 *
 * `Keyword.SUSPEND` is display-only — the engine's `SuspendEnumerator` reads the *parameterized*
 * [KeywordAbility.Suspend] (CR 702.62), never the bare enum, and `CardBuilder.build()` derives the
 * enum from it. So suspend is written once, as `suspend(cost, timeCounters)` — **cost first, the
 * count second** — exactly as on `tsp/cards/SearchForTomorrow.kt`. Trample, by contrast, is a real
 * engine keyword and needs no lowering.
 */
val Gargadon = card("Gargadon") {
    manaCost = "{5}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Beast"
    power = 7
    toughness = 5
    oracleText = "Trample\n" +
        "Suspend 4—{1}{R} (Rather than cast this card from your hand, you may pay {1}{R} and exile it with four time counters on it. At the beginning of your upkeep, remove a time counter. When the last is removed, you may cast it without paying its mana cost. It has haste.)"

    keywords(Keyword.TRAMPLE)

    keywordAbility(KeywordAbility.suspend("{1}{R}", 4))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "128"
        artist = "Chris Seaman"
        imageUri = "https://cards.scryfall.io/normal/front/4/b/4b672c59-7376-455d-961e-ce94d47a5ca4.jpg?1783926844"
    }
}
