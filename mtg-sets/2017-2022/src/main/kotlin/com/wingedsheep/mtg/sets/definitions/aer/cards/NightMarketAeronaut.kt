package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters

/**
 * Night Market Aeronaut
 * {3}{B}
 * Creature — Aetherborn Warrior
 * 2/2
 *
 * Flying
 * Revolt — This creature enters with a +1/+1 counter on it if a permanent left the
 * battlefield under your control this turn.
 *
 * Revolt here is the condition of an enters-with-counters replacement effect, checked against
 * the per-player [Conditions.YouHadPermanentLeaveBattlefieldThisTurn] tracker (same shape as
 * Narnam Renegade).
 *
 * Aether Revolt is the earliest printing (the only other one is the digital-only KLR).
 */
val NightMarketAeronaut = card("Night Market Aeronaut") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Aetherborn Warrior"
    oracleText = "Flying\nRevolt — This creature enters with a +1/+1 counter on it if a permanent left the battlefield under your control this turn."
    power = 2
    toughness = 2

    keywords(Keyword.FLYING)

    replacementEffect(
        EntersWithCounters(
            counterType = CounterType.PLUS_ONE_PLUS_ONE,
            count = 1,
            selfOnly = true,
            condition = Conditions.YouHadPermanentLeaveBattlefieldThisTurn
        )
    )

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "67"
        artist = "Tyler Jacobson"
        flavorText = "\"Death might come tomorrow, but today—today I am alive.\""
        imageUri = "https://cards.scryfall.io/normal/front/2/6/2637f860-01dd-4559-97b3-71c2e7cdbca4.jpg?1783936760"
        ruling("2017-02-09", "Revolt abilities check only whether a permanent you controlled left the battlefield this turn or not. They don't apply multiple times if more than one permanent you controlled left the battlefield. They don't check whether the permanent that left the battlefield is still in the zone it moved to.")
        ruling("2017-02-09", "Revolt abilities don't care why the permanent left the battlefield, who caused it to move, or where it moved to. They're equally satisfied by an artifact you sacrificed to pay a cost, a creature you controlled that was destroyed by Murder, or an enchantment you returned to your hand with Leave in the Dust.")
        ruling("2017-02-09", "Tokens that leave the battlefield will satisfy a revolt ability.")
        ruling("2017-02-09", "Energy counters aren't permanents. Paying {E} won't satisfy a revolt ability.")
    }
}
