package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Stromkirk Mentor (Shadows over Innistrad #137)
 * {3}{B}
 * Creature — Vampire Soldier
 * 4/2
 *
 * When this creature enters, put a +1/+1 counter on another target Vampire you control.
 */
val StromkirkMentor = card("Stromkirk Mentor") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Vampire Soldier"
    power = 4
    toughness = 2
    oracleText = "When this creature enters, put a +1/+1 counter on another target Vampire you control."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val t = target(TargetFilter.Permanent.withSubtype("Vampire").youControl().other())
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "137"
        artist = "Cynthia Sheppard"
        flavorText = "The lineage of Runo Stromkirk is known for producing some of Nephalia's greatest " +
            "fencing masters."
        imageUri = "https://cards.scryfall.io/normal/front/a/a/aa90b7dc-fb90-411e-a762-06a800bae0cf.jpg"
    }
}
