package com.wingedsheep.mtg.sets.definitions.arb.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Bituminous Blast
 * {3}{B}{R}
 * Instant
 * Cascade
 * Bituminous Blast deals 4 damage to target creature.
 *
 * [Keyword.CASCADE] is display-only; the cascade itself is the `Triggers.self.isCast()` trigger
 * feeding [Effects.Cascade] — the same shape as `arb/cards/BloodbraidElf.kt`.
 */
val BituminousBlast = card("Bituminous Blast") {
    manaCost = "{3}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Instant"
    oracleText = "Cascade (When you cast this spell, exile cards from the top of your library until you " +
        "exile a nonland card that costs less. You may cast it without paying its mana cost. Put " +
        "the exiled cards on the bottom in a random order.)\n" +
        "Bituminous Blast deals 4 damage to target creature."

    keywords(Keyword.CASCADE)

    // Cascade — the cast trigger the keyword abbreviates.
    triggeredAbility {
        trigger = Triggers.self.isCast()
        effect = Effects.Cascade
        description = "Cascade"
    }

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.DealDamage(4, creature)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "34"
        artist = "Raymond Swanland"
        imageUri = "https://cards.scryfall.io/normal/front/f/d/fd6a3c48-b01e-4462-a4a1-8d5009a6a844.jpg?1783942435"
    }
}
