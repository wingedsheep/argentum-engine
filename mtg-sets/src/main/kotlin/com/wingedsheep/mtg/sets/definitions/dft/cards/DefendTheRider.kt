package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CrewSaddleContribution
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Defend the Rider — Aetherdrift #157
 * {G} · Instant
 */
val DefendTheRider = card("Defend the Rider") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Choose one —\n" +
        "• Target permanent you control gains hexproof and indestructible until end of turn.\n" +
        "• Create a 1/1 colorless Pilot creature token with \"This token saddles Mounts and crews " +
        "Vehicles as though its power were 2 greater.\""

    spell {
        modal(chooseCount = 1) {
            mode("Grant hexproof and indestructible") {
                val permanent = target(
                    "target permanent you control",
                    TargetPermanent(filter = TargetFilter(GameObjectFilter.Permanent.youControl()))
                )
                effect = Effects.GrantKeyword(Keyword.HEXPROOF, permanent, Duration.EndOfTurn)
                    .then(Effects.GrantKeyword(Keyword.INDESTRUCTIBLE, permanent, Duration.EndOfTurn))
            }
            mode("Create a 1/1 Pilot creature token") {
                effect = Effects.CreateToken(
                    power = 1,
                    toughness = 1,
                    creatureTypes = setOf("Pilot"),
                    imageUri = "https://cards.scryfall.io/normal/front/8/6/8672d795-04f9-4089-9c92-6d6ff628da12.jpg?1783907682",
                    staticAbilities = listOf(CrewSaddleContribution(modifier = 2))
                )
            }
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "157"
        artist = "Raph Lomotan"
        flavorText = "Lagorin would not lose another."
        imageUri = "https://cards.scryfall.io/normal/front/5/9/59ed23a2-6153-47b2-ab73-062195cafb74.jpg?1783907873"
    }
}
