package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CrewSaddleContribution
import com.wingedsheep.sdk.scripting.CrewSaddleCost
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Prodigy's Prototype — Kamigawa: Neon Dynasty #231
 * {1}{W}{U} · Artifact — Vehicle · 3/4
 *
 * Whenever one or more Vehicles you control attack, create a 1/1 colorless Pilot creature token
 * with "This token crews Vehicles as though its power were 2 greater."
 * Crew 2
 *
 * The batch attack trigger is `Triggers.you.attacks(with)` (Metropolis Angel's shape): it fires
 * once per attack declaration when at least one declared attacker is a Vehicle, so two attacking
 * Vehicles still make one Pilot. The Pilot is Born to Drive's crew-only token.
 */
val ProdigysPrototype = card("Prodigy's Prototype") {
    manaCost = "{1}{W}{U}"
    colorIdentity = "WU"
    typeLine = "Artifact — Vehicle"
    power = 3
    toughness = 4
    oracleText = "Whenever one or more Vehicles you control attack, create a 1/1 colorless Pilot " +
        "creature token with \"This token crews Vehicles as though its power were 2 greater.\"\n" +
        "Crew 2 (Tap any number of creatures you control with total power 2 or more: This Vehicle " +
        "becomes an artifact creature until end of turn.)"

    triggeredAbility {
        trigger = Triggers.you.attacks(GameObjectFilter.Permanent.withSubtype(Subtype.VEHICLE))
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            creatureTypes = setOf("Pilot"),
            imageUri = "https://cards.scryfall.io/normal/front/b/e/be84f259-2809-48c9-9c70-861437f08c23.jpg?1783923717",
            staticAbilities = listOf(CrewSaddleContribution(modifier = 2, costs = setOf(CrewSaddleCost.CREW)))
        )
        description = "Whenever one or more Vehicles you control attack, create a 1/1 colorless Pilot " +
            "creature token with \"This token crews Vehicles as though its power were 2 greater.\""
    }

    keywordAbility(KeywordAbility.crew(2))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "231"
        artist = "Ryan Pancoast"
        imageUri = "https://cards.scryfall.io/normal/front/b/e/be046e0a-6509-450b-b34f-29d5a5e3472e.jpg?1783923831"
    }
}
