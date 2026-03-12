package com.wingedsheep.mtg.sets.definitions.dominaria.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Voltaic Servant
 * {2}
 * Artifact Creature — Construct
 * 1/3
 * At the beginning of your end step, untap target artifact.
 */
val VoltaicServant = card("Voltaic Servant") {
    manaCost = "{2}"
    typeLine = "Artifact Creature — Construct"
    power = 1
    toughness = 3
    oracleText = "At the beginning of your end step, untap target artifact."

    triggeredAbility {
        trigger = Triggers.YourEndStep
        val t = target("target", Targets.Artifact)
        effect = Effects.Untap(t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "236"
        artist = "Jonas De Ro"
        flavorText = "\"A missing piece in search of a puzzle.\""
        imageUri = "https://cards.scryfall.io/normal/front/2/8/28564ac6-8b9b-4b99-9630-8fb3158d354c.jpg?1562733045"
    }
}
