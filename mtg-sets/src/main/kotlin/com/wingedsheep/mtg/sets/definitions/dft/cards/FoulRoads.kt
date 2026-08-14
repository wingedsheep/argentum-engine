package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CrewSaddleContribution
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.conditions.Exists
import com.wingedsheep.sdk.scripting.effects.AddManaEffect
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.references.Player

private val FoulRoadsMountOrVehicle = GameObjectFilter(
    cardPredicates = listOf(
        CardPredicate.Or(
            listOf(
                CardPredicate.HasSubtype(Subtype("Mount")),
                CardPredicate.HasSubtype(Subtype.VEHICLE)
            )
        )
    )
)

val FoulRoads = card("Foul Roads") {
    colorIdentity = "B"
    typeLine = "Land"
    oracleText = "This land enters tapped unless you control a Mount or Vehicle.\n" +
        "{T}: Add {B}.\n" +
        "{1}{B}, {T}, Sacrifice this land: Create a 1/1 colorless Pilot creature token with " +
        "\"This token saddles Mounts and crews Vehicles as though its power were 2 greater.\" " +
        "Activate only as a sorcery."

    replacementEffect(
        EntersTapped(
            unlessCondition = Exists(
                player = Player.You,
                zone = Zone.BATTLEFIELD,
                filter = FoulRoadsMountOrVehicle
            )
        )
    )

    activatedAbility {
        cost = Costs.Tap
        effect = AddManaEffect(Color.BLACK)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{B}"), Costs.Tap, Costs.SacrificeSelf)
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            creatureTypes = setOf("Pilot"),
            imageUri = "https://cards.scryfall.io/normal/front/8/6/8672d795-04f9-4089-9c92-6d6ff628da12.jpg?1783907682",
            staticAbilities = listOf(CrewSaddleContribution(modifier = 2))
        )
        timing = TimingRule.SorcerySpeed
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "255"
        artist = "Borja Pindado"
        imageUri = "https://cards.scryfall.io/normal/front/a/3/a37c026a-c89f-41f3-8812-424124dd3760.jpg?1783907842"
    }
}
