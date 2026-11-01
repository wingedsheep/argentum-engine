package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Crush Dissent — War of the Spark #47 (canonical printing)
 * {3}{U}
 * Instant
 * Counter target spell unless its controller pays {2}.
 * Amass Zombies 2.
 *
 * You amass even if the spell's controller pays {2} (2019-05-03 ruling).
 */
val CrushDissent = card("Crush Dissent") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Counter target spell unless its controller pays {2}.\n" +
        "Amass Zombies 2. (Put two +1/+1 counters on an Army you control. It's also a Zombie. " +
        "If you don't control an Army, create a 0/0 black Zombie Army creature token first.)"

    spell {
        target(TargetFilter.SpellOnStack)
        effect = Effects.CounterUnlessPays("{2}") then Effects.Amass(2, "Zombie")
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "47"
        artist = "Mike Bierek"
        flavorText = "\"All I ask is complete obedience.\"\n—Nicol Bolas"
        imageUri = "https://cards.scryfall.io/normal/front/9/4/94c70f23-0ca9-425e-a53a-6c09921c0075.jpg?1783933466"
        ruling("2019-05-03", "You amass 2 even if the controller of the spell pays {2}.")
        ruling(
            "2023-06-16",
            "Some spells and abilities that amass Zombies may require targets. If each target chosen is an illegal target as that spell or ability tries to resolve, it won't resolve. You won't amass Zombies."
        )
    }
}
