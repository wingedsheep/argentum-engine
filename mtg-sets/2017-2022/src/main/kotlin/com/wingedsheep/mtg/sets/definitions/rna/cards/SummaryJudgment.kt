package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Summary Judgment
 * {1}{W}
 * Instant
 * Summary Judgment deals 3 damage to target tapped creature.
 * Addendum — If you cast this spell during your main phase, it deals 5 damage instead.
 *
 * The "instead" is one damage amount chosen at resolution: [Conditions.WasCast] excludes copies
 * (never cast), and [Conditions.IsYourMainPhase] reads the cast-time phase, since a phase only ends
 * with an empty stack.
 */
val SummaryJudgment = card("Summary Judgment") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Summary Judgment deals 3 damage to target tapped creature.\n" +
        "Addendum — If you cast this spell during your main phase, it deals 5 damage instead."

    spell {
        val creature = target(TargetFilter.TappedCreature)

        effect = Effects.DealDamage(
            DynamicAmounts.conditional(
                condition = Conditions.All(Conditions.WasCast, Conditions.IsYourMainPhase),
                ifTrue = DynamicAmounts.fixed(5),
                ifFalse = DynamicAmounts.fixed(3),
            ),
            creature,
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "24"
        artist = "Deruchenko Alexander"
        imageUri = "https://cards.scryfall.io/normal/front/c/0/c0b20fec-8373-4c6c-b3c1-ee7cff64dd37.jpg?1783933716"
    }
}
