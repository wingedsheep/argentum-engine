package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.conditions.WasKicked

/**
 * Ghastly Gloomhunter — Zendikar Rising #103
 * {1}{B} · Creature — Zombie Bat · 1/1
 *
 * Kicker {3}{B}
 * Flying, lifelink
 * If this creature was kicked, it enters with two +1/+1 counters on it.
 *
 * "Enters with counters" is a replacement effect, not an ETB trigger.
 */
val GhastlyGloomhunter = card("Ghastly Gloomhunter") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie Bat"
    oracleText = "Kicker {3}{B} (You may pay an additional {3}{B} as you cast this spell.)\n" +
        "Flying, lifelink\n" +
        "If this creature was kicked, it enters with two +1/+1 counters on it."
    power = 1
    toughness = 1

    keywordAbility(KeywordAbility.kicker("{3}{B}"))
    keywords(Keyword.FLYING, Keyword.LIFELINK)

    replacementEffect(
        EntersWithCounters(
            counterType = CounterType.PLUS_ONE_PLUS_ONE,
            count = 2,
            selfOnly = true,
            condition = WasKicked,
        )
    )

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "103"
        artist = "Zezhou Chen"
        flavorText = "Stinking, silent death."
        imageUri = "https://cards.scryfall.io/normal/front/f/2/f272ac54-fea8-4044-83e2-f8fdc10f467d.jpg"
    }
}
