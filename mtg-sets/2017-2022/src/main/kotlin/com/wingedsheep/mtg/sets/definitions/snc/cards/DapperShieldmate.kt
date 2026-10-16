package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.ModifyStats

/**
 * Dapper Shieldmate
 * {3}{W}
 * Creature — Human Soldier
 * 2/2
 * This creature enters with a shield counter on it. (If it would be dealt damage or destroyed, remove a shield counter from it instead.)
 * During your turn, this creature gets +2/+0.
 *
 * The shield counter is the engine's CR 122.1c counter — its replacement + prevention pair is
 * wired at the damage and destroy chokepoints, so the card only needs the
 * [EntersWithCounters] as-enters replacement.
 */
val DapperShieldmate = card("Dapper Shieldmate") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Soldier"
    oracleText = "This creature enters with a shield counter on it. (If it would be dealt damage or destroyed, remove a shield counter from it instead.)\nDuring your turn, this creature gets +2/+0."
    power = 2
    toughness = 2

    replacementEffect(
        EntersWithCounters(
            counterType = CounterType.SHIELD,
            count = 1,
            selfOnly = true,
        )
    )

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = ModifyStats(2, 0, Filters.Self),
            condition = Conditions.IsYourTurn,
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "9"
        artist = "Greg Opalinski"
        flavorText = "Descended from Old Capenna's paladins, the Brokers are masters of defensive combat."
        imageUri = "https://cards.scryfall.io/normal/front/d/6/d6ce9c33-4ab3-43c9-b6bc-ee2d40d8f9b6.jpg?1783923160"
    }
}
