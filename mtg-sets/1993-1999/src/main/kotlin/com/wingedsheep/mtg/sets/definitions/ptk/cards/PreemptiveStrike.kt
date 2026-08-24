package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Preemptive Strike
 * {1}{U}
 * Instant
 *
 * Counter target creature spell.
 */
val PreemptiveStrike = card("Preemptive Strike") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Counter target creature spell."

    spell {
        target("target", Targets.CreatureSpell)
        effect = Effects.CounterSpell()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "50"
        artist = "Jiaming"
        imageUri = "https://cards.scryfall.io/normal/front/c/2/c2314bf1-b22d-48c2-860f-e1081f56296b.jpg"
    }
}
