package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.WardCost

/**
 * Phyrexian Fleshgorger
 * {7}
 * Artifact Creature — Phyrexian Wurm
 * 7/5
 * Prototype {1}{B}{B} — 3/3
 * Menace, lifelink
 * Ward—Pay life equal to this creature's power.
 *
 * The ward cost is [WardCost.DynamicLife] over the source's power, evaluated when the ward
 * trigger resolves (last-known power if Fleshgorger has left) — so a prototyped 3/3 asks for 3.
 */
val PhyrexianFleshgorger = card("Phyrexian Fleshgorger") {
    manaCost = "{7}"
    colorIdentity = "B"
    typeLine = "Artifact Creature — Phyrexian Wurm"
    power = 7
    toughness = 5
    oracleText = "Prototype {1}{B}{B} — 3/3 (You may cast this spell with different mana cost, color, and size. It keeps its abilities and types.)\n" +
        "Menace, lifelink\n" +
        "Ward—Pay life equal to this creature's power."

    keywordAbility(KeywordAbility.prototype("{1}{B}{B}", 3, 3))

    keywords(Keyword.MENACE, Keyword.LIFELINK)

    keywordAbility(KeywordAbility.Ward(WardCost.DynamicLife(DynamicAmounts.sourcePower())))

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "121"
        artist = "Steve Prescott"
        imageUri = "https://cards.scryfall.io/normal/front/6/2/62d37423-3445-412a-9abd-0480da404637.jpg?1783920074"
        ruling("2022-10-14", "The amount of life an opponent needs to pay for that spell or ability to resolve is equal to Phyrexian Fleshgorger's power at the time the ward ability resolves, which may be different from its power when the ward ability triggered. If Phyrexian Fleshgorger is no longer on the battlefield at the time the ward ability resolves, use the power it had the last time it was on the battlefield to determine how much life that player must pay (though if Phyrexian Fleshgorger was the only target, the spell has no legal targets anymore and hence it would not resolve even if they pay the life).")
    }
}
