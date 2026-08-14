package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.maxSpeed
import com.wingedsheep.sdk.dsl.startYourEngines
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Nesting Bot — Aetherdrift #22
 * {W} · Artifact Creature — Robot · 1/1
 *
 * Start your engines!
 * When this creature dies, create a 1/1 colorless Servo artifact creature token.
 * Max speed — This creature gets +1/+0.
 *
 * The dies trigger is unconditional — it is *not* under the max-speed gate, so the Servo arrives
 * whatever the controller's speed is. Only the +1/+0 is gated, as a [ModifyStats] static that the
 * projection re-evaluates every pass; speed dropping is impossible today, but the gate is read
 * rather than latched either way.
 */
val NestingBot = card("Nesting Bot") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Artifact Creature — Robot"
    power = 1
    toughness = 1
    oracleText = "Start your engines! (If you have no speed, it starts at 1. It increases once on " +
        "each of your turns when an opponent loses life. Max speed is 4.)\n" +
        "When this creature dies, create a 1/1 colorless Servo artifact creature token.\n" +
        "Max speed — This creature gets +1/+0."

    startYourEngines()

    triggeredAbility {
        trigger = Triggers.Dies
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            creatureTypes = setOf("Servo"),
            artifactToken = true,
            imageUri = "https://cards.scryfall.io/normal/front/1/0/10de413b-3307-4bc7-bb21-f177543d7e21.jpg?1783907678"
        )
        description = "When this creature dies, create a 1/1 colorless Servo artifact creature token."
    }

    maxSpeed {
        staticAbility { ability = ModifyStats(1, 0, GroupFilter.source()) }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "22"
        artist = "Racrufi"
        imageUri = "https://cards.scryfall.io/normal/front/7/8/7829c0ae-f72f-4195-ad43-775d7218565c.jpg?1783907917"
    }
}
