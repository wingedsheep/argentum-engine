package com.wingedsheep.mtg.sets.definitions.p02.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Jagged Lightning
 * {3}{R}{R}
 * Sorcery
 * Jagged Lightning deals 3 damage to each of two target creatures.
 */
val JaggedLightning = card("Jagged Lightning") {
    manaCost = "{3}{R}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Jagged Lightning deals 3 damage to each of two target creatures."

    spell {
        targets(TargetFilter.Creature, count = 2)
        effect = Effects.ForEachTarget(
            Effects.DealDamage(3, EffectTarget.ContextTarget(0))
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "106"
        artist = "Michael Weaver"
        imageUri = "https://cards.scryfall.io/normal/front/1/4/148e6704-9cf0-45cf-9bab-db318c016593.jpg?1783946462"
    }
}
