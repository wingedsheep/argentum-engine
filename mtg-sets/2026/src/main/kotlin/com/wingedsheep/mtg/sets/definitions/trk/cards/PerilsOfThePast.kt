package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Perils of the Past — Star Trek #161
 * {1}{R} · Instant
 *
 * Choose one —
 * • Use the Relic — Target creature gets +3/+2 until end of turn.
 * • Destroy the Relic — Destroy target artifact.
 */
val PerilsOfThePast = card("Perils of the Past") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Choose one —\n" +
        "• Use the Relic — Target creature gets +3/+2 until end of turn.\n" +
        "• Destroy the Relic — Destroy target artifact."

    spell {
        modal(chooseCount = 1) {
            mode("Use the Relic — Target creature gets +3/+2 until end of turn") {
                val creature = target(TargetFilter.Creature)
                effect = Effects.ModifyStats(3, 2, creature)
            }
            mode("Destroy the Relic — Destroy target artifact") {
                val artifact = target(TargetFilter.Artifact)
                effect = Effects.Destroy(artifact)
            }
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "161"
        artist = "Zoltan Boros"
        imageUri = "https://cards.scryfall.io/normal/front/3/d/3df10e9a-efb6-4945-9b1b-29200688702a.jpg?1785981548"
    }
}
