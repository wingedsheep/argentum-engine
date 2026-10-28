package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CanAttackDespiteDefender
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.conditions.WasKicked

/**
 * Skyclave Sentinel — Zendikar Rising #253
 * {3} · Artifact Creature — Gargoyle · 2/3
 *
 * Kicker {4}
 * Flying, defender
 * If this creature was kicked, it enters with two +1/+1 counters on it.
 * As long as this creature has a +1/+1 counter on it, it can attack as though it didn't have defender.
 */
val SkyclaveSentinel = card("Skyclave Sentinel") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Gargoyle"
    power = 2
    toughness = 3
    oracleText = "Kicker {4} (You may pay an additional {4} as you cast this spell.)\n" +
        "Flying, defender\n" +
        "If this creature was kicked, it enters with two +1/+1 counters on it.\n" +
        "As long as this creature has a +1/+1 counter on it, it can attack as though it didn't have defender."

    keywordAbility(KeywordAbility.kicker("{4}"))
    keywords(Keyword.FLYING, Keyword.DEFENDER)

    // "Enters with counters" is a replacement effect, not an ETB trigger.
    replacementEffect(EntersWithCounters(
        counterType = CounterType.PLUS_ONE_PLUS_ONE,
        count = 2,
        selfOnly = true,
        condition = WasKicked
    ))

    // Any +1/+1 counter counts — kicked or placed by another effect.
    staticAbility {
        ability = CanAttackDespiteDefender(
            condition = Conditions.SourceHasCounter(CounterType.PLUS_ONE_PLUS_ONE)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "253"
        artist = "Joseph Meehan"
        imageUri = "https://cards.scryfall.io/normal/front/f/a/fac8809a-77cd-4090-8a89-12121a8da676.jpg?1783929309"
    }
}
