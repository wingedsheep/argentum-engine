package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * S.H.I.E.L.D. Deployment Drone
 * {2}{U}
 * Artifact Creature — Robot
 * 2/2
 *
 * Flying
 * When this creature enters, create a 1/1 white Soldier creature token.
 */
val ShieldDeploymentDrone = card("S.H.I.E.L.D. Deployment Drone") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Artifact Creature — Robot"
    power = 2
    toughness = 2
    oracleText = "Flying\n" +
        "When this creature enters, create a 1/1 white Soldier creature token."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Soldier"),
            imageUri = "https://cards.scryfall.io/normal/front/e/c/ecd686bf-d14b-491c-b0c5-88fc8f0472f9.jpg?1783902804"
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "73"
        artist = "Paulius Daščioras"
        flavorText = "One of the core capabilities of S.H.I.E.L.D. is deploying agents anywhere within minutes."
        imageUri = "https://cards.scryfall.io/normal/front/c/3/c3d0f02f-dfaf-47b6-8053-514417f4dfe2.jpg?1783902953"
    }
}
