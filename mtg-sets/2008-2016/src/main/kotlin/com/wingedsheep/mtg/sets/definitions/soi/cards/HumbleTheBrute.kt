package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Humble the Brute — Shadows over Innistrad #23
 * {4}{W} · Instant
 *
 * Destroy target creature with power 4 or greater. Investigate.
 *
 * The Clue rides on the whole spell resolving: if the only target is illegal on resolution the
 * spell doesn't resolve and you don't investigate (2016-04-08 ruling).
 */
val HumbleTheBrute = card("Humble the Brute") {
    manaCost = "{4}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Destroy target creature with power 4 or greater.\n" +
        "Investigate. (Create a Clue token. It's an artifact with \"{2}, Sacrifice this token: Draw a card.\")"

    spell {
        val creature = target(TargetFilter.Creature.powerAtLeast(4))
        effect = Effects.Destroy(creature) then Effects.Investigate()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "23"
        artist = "Daarken"
        flavorText = "\"The element of surprise only works when it's backed up by a bolt between the eyes.\""
        imageUri = "https://cards.scryfall.io/normal/front/7/c/7c105686-8b45-494a-b9ef-8aa267bb1b5a.jpg?1783937817"
        ruling(
            "2016-04-08",
            "You can't cast a spell without choosing legal targets. If all of those targets become " +
                "illegal, the spell doesn't resolve and you won't investigate.",
        )
    }
}
