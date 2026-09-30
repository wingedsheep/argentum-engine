package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Reiterating Bolt
 * {1}{R}
 * Sorcery
 * Replicate—Pay {E}{E}{E}.
 * Reiterating Bolt deals 3 damage to target creature or planeswalker.
 *
 * Paying replicate N times is one payment of 3N energy, announced with the cast.
 */
val ReiteratingBolt = card("Reiterating Bolt") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Replicate—Pay {E}{E}{E}. (When you cast this spell, copy it for each time you paid its " +
        "replicate cost. You may choose new targets for the copies.)\n" +
        "Reiterating Bolt deals 3 damage to target creature or planeswalker."

    keywordAbility(KeywordAbility.replicate(Costs.additional.PayPlayerCounters(CounterType.ENERGY, 3)))

    spell {
        val t = target(Targets.CreatureOrPlaneswalker)
        effect = Effects.DealDamage(3, t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "134"
        artist = "G-host Lee"
        imageUri = "https://cards.scryfall.io/normal/front/5/1/51f57902-85d1-4c40-b79c-6cffafb4557a.jpg?1783911267"
        ruling(
            "2024-06-07",
            "A copy of a spell can be countered like any other spell, but it must be countered individually. " +
                "Countering a spell with replicate won't affect the copies."
        )
    }
}
