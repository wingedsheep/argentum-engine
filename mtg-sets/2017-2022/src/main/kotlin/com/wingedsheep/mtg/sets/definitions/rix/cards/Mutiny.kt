package com.wingedsheep.mtg.sets.definitions.rix.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Mutiny
 * {R}
 * Sorcery
 *
 * Target creature an opponent controls deals damage equal to its power to another target
 * creature that player controls.
 *
 * One two-target requirement constrained to creatures the same opponent controls
 * (`sameController = true`, count = 2 — distinct targets enforce "another"); the first
 * creature deals damage equal to its power to the second. Same shape as Breaking of the
 * Fellowship.
 */
val Mutiny = card("Mutiny") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Target creature an opponent controls deals damage equal to its power to " +
        "another target creature that player controls."

    spell {
        val (source, victim) = targets(TargetFilter.CreatureOpponentControls, count = 2, sameController = true)
        effect = Effects.DealDamage(
            amount = DynamicAmounts.powerOf(source),
            target = victim,
            damageSource = source
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "106"
        flavorText = "Pirate captains rarely enjoy a peaceful retirement."
        artist = "Gabor Szikszai"
        imageUri = "https://cards.scryfall.io/normal/front/0/1/010711a0-5e95-4a8c-816f-e314f2a909ef.jpg?1783935297"
    }
}
