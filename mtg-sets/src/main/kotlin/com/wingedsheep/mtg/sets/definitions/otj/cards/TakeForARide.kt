package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration

/**
 * Take for a Ride {2}{R}
 * Sorcery
 *
 * Take for a Ride has flash as long as you've committed a crime this turn.
 * (Targeting opponents, anything they control, and/or cards in their graveyards is a crime.)
 * Gain control of target creature until end of turn. Untap that creature. It gains haste until end of turn.
 *
 * A "Threaten" variant: gain control / untap / grant haste, all threading the single
 * creature target. The conditional-flash clause is the card-level `conditionalFlash`
 * field (gated on [Conditions.YouCommittedCrimeThisTurn]); the crime-this-turn tracker
 * is consulted whenever the engine recomputes castable timing.
 */
val TakeForARide = card("Take for a Ride") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Take for a Ride has flash as long as you've committed a crime this turn. " +
        "(Targeting opponents, anything they control, and/or cards in their graveyards is a crime.)\n" +
        "Gain control of target creature until end of turn. Untap that creature. It gains haste until end of turn."

    conditionalFlash = Conditions.YouCommittedCrimeThisTurn

    spell {
        val t = target("target creature", Targets.Creature)
        effect = Effects.Composite(
            Effects.GainControl(t, Duration.EndOfTurn),
            Effects.Untap(t),
            Effects.GrantKeyword(Keyword.HASTE, t)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "148"
        artist = "Artur Treffner"
        flavorText = "\"Your horse deserves a braver rider!\""
        imageUri = "https://cards.scryfall.io/normal/front/c/8/c8e2ff9c-0e98-46f9-a33c-739388c5f3d0.jpg?1712355858"
    }
}
