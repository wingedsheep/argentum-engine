package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Magda, the Hoardmaster
 * {1}{R}
 * Legendary Creature — Dwarf Berserker
 * 2/2
 * Whenever you commit a crime, create a tapped Treasure token. This ability triggers only
 * once each turn.
 * Sacrifice three Treasures: Create a 4/4 red Scorpion Dragon creature token with flying and
 * haste. Activate only as a sorcery.
 *
 * Modeling notes:
 * - The crime payoff is [Triggers.YouCommitCrime] with `oncePerTurn = true` (CR rules the
 *   ability triggers at most once per turn). The Treasure enters tapped.
 * - The activated ability pays by sacrificing three Treasures ([Costs.SacrificeMultiple] over
 *   the Treasure subtype) and is restricted to sorcery speed via [TimingRule.SorcerySpeed].
 */
val MagdaTheHoardmaster = card("Magda, the Hoardmaster") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Dwarf Berserker"
    power = 2
    toughness = 2
    oracleText = "Whenever you commit a crime, create a tapped Treasure token. This ability " +
        "triggers only once each turn. (Targeting opponents, anything they control, and/or " +
        "cards in their graveyards is a crime.)\n" +
        "Sacrifice three Treasures: Create a 4/4 red Scorpion Dragon creature token with " +
        "flying and haste. Activate only as a sorcery."

    triggeredAbility {
        trigger = Triggers.YouCommitCrime
        oncePerTurn = true
        effect = Effects.CreateTreasure(1, tapped = true)
    }

    activatedAbility {
        cost = Costs.SacrificeMultiple(3, GameObjectFilter.Artifact.withSubtype("Treasure"))
        timing = TimingRule.SorcerySpeed
        // The token's name ("Scorpion Dragon") derives from its creature types, matching the codebase
        // convention for type-named tokens (so the mtgish emitter renders it identically — AUTO tier).
        effect = Effects.CreateToken(
            power = 4,
            toughness = 4,
            colors = setOf(Color.RED),
            creatureTypes = setOf("Scorpion", "Dragon"),
            keywords = setOf(Keyword.FLYING, Keyword.HASTE)
        )
        description = "Sacrifice three Treasures: Create a 4/4 red Scorpion Dragon creature " +
            "token with flying and haste. Activate only as a sorcery."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "133"
        artist = "Diego Gisbert"
        imageUri = "https://cards.scryfall.io/normal/front/4/4/4443d112-209b-49ec-bc40-3a11dcdb092e.jpg?1712355792"
    }
}
