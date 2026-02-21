package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ModifyStatsEffect
import com.wingedsheep.sdk.scripting.effects.TapUntapEffect

/**
 * Inspirit
 * {2}{W}
 * Instant
 * Untap target creature. It gets +2/+4 until end of turn.
 */
val Inspirit = card("Inspirit") {
    manaCost = "{2}{W}"
    typeLine = "Instant"
    oracleText = "Untap target creature. It gets +2/+4 until end of turn."

    spell {
        val t = target("target", com.wingedsheep.sdk.scripting.targets.TargetCreature())
        effect = TapUntapEffect(t, tap = false) then
                ModifyStatsEffect(2, 4, t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "41"
        artist = "Keith Garletts"
        flavorText = "\"You're not done yet.\"\n—Akroma, to Kamahl"
        imageUri = "https://cards.scryfall.io/large/front/5/5/55e0e300-db79-4328-ba1d-9c3910e47f52.jpg?1595099724"
    }
}
