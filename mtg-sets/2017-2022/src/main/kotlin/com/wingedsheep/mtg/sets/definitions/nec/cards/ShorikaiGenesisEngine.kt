package com.wingedsheep.mtg.sets.definitions.nec.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CrewSaddleContribution
import com.wingedsheep.sdk.scripting.CrewSaddleCost
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Shorikai, Genesis Engine — Neon Dynasty Commander #4 (canonical printing)
 * {2}{W}{U} · Legendary Artifact — Vehicle · 8/8
 *
 * {1}, {T}: Draw two cards, then discard a card. Create a 1/1 colorless Pilot creature token with
 * "This token crews Vehicles as though its power were 2 greater."
 * Crew 8
 *
 * The Pilot is the Prodigy's Prototype token: a crew-only [CrewSaddleContribution] of +2.
 */
val ShorikaiGenesisEngine = card("Shorikai, Genesis Engine") {
    manaCost = "{2}{W}{U}"
    colorIdentity = "WU"
    typeLine = "Legendary Artifact — Vehicle"
    power = 8
    toughness = 8
    oracleText = "{1}, {T}: Draw two cards, then discard a card. Create a 1/1 colorless Pilot " +
        "creature token with \"This token crews Vehicles as though its power were 2 greater.\"\n" +
        "Crew 8 (Tap any number of creatures you control with total power 8 or more: This Vehicle " +
        "becomes an artifact creature until end of turn.)"

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Tap)
        effect = Patterns.Hand.loot(draw = 2, discard = 1) then
            Effects.CreateToken(
                power = 1,
                toughness = 1,
                creatureTypes = setOf("Pilot"),
                imageUri = "https://cards.scryfall.io/normal/front/b/e/be84f259-2809-48c9-9c70-861437f08c23.jpg?1783923717",
                staticAbilities = listOf(CrewSaddleContribution(modifier = 2, costs = setOf(CrewSaddleCost.CREW)))
            )
    }

    keywordAbility(KeywordAbility.crew(8))

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "4"
        artist = "Wisnu Tan"
        imageUri = "https://cards.scryfall.io/normal/front/9/6/969ac7dd-f3aa-4888-9ff0-d16a31b5e7a9.jpg?1783923999"
    }
}
