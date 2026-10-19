package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CrewSaddleContribution
import com.wingedsheep.sdk.scripting.CrewSaddleCost
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Reckoner Bankbuster — Kamigawa: Neon Dynasty #255
 * {2} · Artifact — Vehicle · 4/4
 *
 * This Vehicle enters with three charge counters on it.
 * {2}, {T}, Remove a charge counter from this Vehicle: Draw a card. Then if there are no charge
 * counters on this Vehicle, create a Treasure token and a 1/1 colorless Pilot creature token with
 * "This token crews Vehicles as though its power were 2 greater."
 * Crew 3
 *
 * The counter is removed as a cost (Serum Tank's shape); the "then if" is checked on resolution
 * against the Vehicle's current counters — or, if it has left the battlefield, the counters it
 * had as it last existed there (the `EntityProperty` read falls back to last-known information).
 * The Pilot is crew-only, like Born to Drive's — NEO predates saddle.
 */
val ReckonerBankbuster = card("Reckoner Bankbuster") {
    manaCost = "{2}"
    typeLine = "Artifact — Vehicle"
    power = 4
    toughness = 4
    oracleText = "This Vehicle enters with three charge counters on it.\n" +
        "{2}, {T}, Remove a charge counter from this Vehicle: Draw a card. Then if there are no " +
        "charge counters on this Vehicle, create a Treasure token and a 1/1 colorless Pilot " +
        "creature token with \"This token crews Vehicles as though its power were 2 greater.\"\n" +
        "Crew 3 (Tap any number of creatures you control with total power 3 or more: This " +
        "Vehicle becomes an artifact creature until end of turn.)"

    replacementEffect(
        EntersWithCounters(
            counterType = CounterType.CHARGE,
            count = 3,
            selfOnly = true
        )
    )

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{2}"),
            Costs.Tap,
            Costs.RemoveCounterFromSelf(CounterType.CHARGE, 1)
        )
        effect = Effects.DrawCards(1) then
            Effects.If(
                condition = Conditions.SourceCounterCountAtMost(CounterType.CHARGE, 0),
                then = Effects.CreateTreasure() then
                    Effects.CreateToken(
                        power = 1,
                        toughness = 1,
                        creatureTypes = setOf("Pilot"),
                        imageUri = "https://cards.scryfall.io/normal/front/b/e/be84f259-2809-48c9-9c70-861437f08c23.jpg?1783923717",
                        staticAbilities = listOf(
                            CrewSaddleContribution(modifier = 2, costs = setOf(CrewSaddleCost.CREW))
                        )
                    )
            )
        description = "{2}, {T}, Remove a charge counter: Draw a card. Then if there are no charge " +
            "counters on this Vehicle, create a Treasure and a 1/1 Pilot token."
    }

    keywordAbility(KeywordAbility.crew(3))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "255"
        artist = "Steve Prescott"
        imageUri = "https://cards.scryfall.io/normal/front/2/7/279acd17-6c17-427b-a69d-fc02442ff4a3.jpg?1783923822"
    }
}
