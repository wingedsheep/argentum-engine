package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Bring the Ending
 * {1}{U}
 * Instant
 * Counter target spell unless its controller pays {2}.
 * Corrupted — Counter that spell instead if its controller has three or more poison counters.
 *
 * The corrupted clause is keyed to the *spell's controller*, not to "an opponent", so it is
 * [Conditions.PoisonCountersAtLeast] over `Player.ControllerOf("target")` choosing between a hard
 * counter and the {2} tax.
 */
val BringTheEnding = card("Bring the Ending") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Counter target spell unless its controller pays {2}.\n" +
        "Corrupted — Counter that spell instead if its controller has three or more poison counters."

    spell {
        target(TargetFilter.SpellOnStack)
        effect = Effects.If(
            condition = Conditions.PoisonCountersAtLeast(3, Player.ControllerOf("target spell")),
            then = Effects.CounterSpell(),
            otherwise = Effects.CounterUnlessPays("{2}"),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "44"
        artist = "L.A. Draws"
        flavorText = "Kaya watched in horror as the illusion vanished from her hands. Jace held the real " +
            "Sylex—and had activated it before phyresis claimed his mind."
        imageUri = "https://cards.scryfall.io/normal/front/b/a/ba9d9d26-0c76-4a09-aa25-b32854e70c0b.jpg?1783918068"
    }
}
