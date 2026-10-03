package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.EntersWithKeywords
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.conditions.WasKicked

/**
 * Voidpouncer
 * {1}{R}
 * Creature — Eldrazi
 * 3/1
 * Devoid
 * Kicker {2}{C}
 * If this creature was kicked, it enters with two +1/+1 counters and a trample counter on it
 * and with haste.
 */
val Voidpouncer = card("Voidpouncer") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Eldrazi"
    power = 3
    toughness = 1
    oracleText = "Devoid (This card has no color.)\n" +
        "Kicker {2}{C} (You may pay an additional {2}{C} as you cast this spell.)\n" +
        "If this creature was kicked, it enters with two +1/+1 counters and a trample counter on it and with haste."

    keywords(Keyword.DEVOID)
    keywordAbility(KeywordAbility.kicker("{2}{C}"))

    // "Enters with … counters … and with haste" is a replacement effect (CR 614.1c), not an
    // ETB trigger: a kicked Voidpouncer is a 5/3 trampling haste creature the moment it enters.
    replacementEffect(EntersWithCounters(
        counterType = CounterType.PLUS_ONE_PLUS_ONE,
        count = 2,
        selfOnly = true,
        condition = WasKicked
    ))
    replacementEffect(EntersWithCounters(
        counterType = CounterType.TRAMPLE,
        count = 1,
        selfOnly = true,
        condition = WasKicked
    ))
    replacementEffect(EntersWithKeywords(
        keywords = listOf(Keyword.HASTE),
        selfOnly = true,
        condition = WasKicked
    ))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "143"
        artist = "Michele Giorgi"
        imageUri = "https://cards.scryfall.io/normal/front/1/f/1f9b8532-4f1a-4653-9cbb-befba8169e5a.jpg?1783911263"
    }
}
