package com.wingedsheep.mtg.sets.definitions.nph.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Volt Charge
 * {2}{R}
 * Instant
 *
 * Volt Charge deals 3 damage to any target. Proliferate.
 *
 * Canonical printing is New Phyrexia (earliest real expansion); ONE and DDL carry `Printing` rows.
 * If the target is illegal on resolution the spell doesn't resolve, so no proliferate (2011-06-01).
 */
val VoltCharge = card("Volt Charge") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Volt Charge deals 3 damage to any target. Proliferate. (Choose any number of permanents " +
        "and/or players, then give each another counter of each kind already there.)"
    spell {
        val t = target(Targets.Any)
        effect = Effects.DealDamage(3, t) then Effects.Proliferate()
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "100"
        artist = "Jana Schirmer & Johannes Voss"
        imageUri = "https://cards.scryfall.io/normal/front/a/a/aa88011c-a19d-4faa-8da6-86b9980cd571.jpg?1783941304"
        ruling(
            "2011-06-01",
            "If the permanent or player is an illegal target when Volt Charge tries to resolve, it won't " +
                "resolve and none of its effects will happen. You won't proliferate."
        )
    }
}
