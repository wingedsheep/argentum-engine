package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Cinderclasm — Zendikar Rising #136
 * {1}{R} · Instant
 *
 * The kicked mode replaces the 1-damage sweep wholesale, so it rides the `kickerEffect` branch.
 */
val Cinderclasm = card("Cinderclasm") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Kicker {R} (You may pay an additional {R} as you cast this spell.)\n" +
        "Cinderclasm deals 1 damage to each creature. If it was kicked, it deals 2 damage to each creature instead."

    keywordAbility(KeywordAbility.kicker("{R}"))

    spell {
        effect = Effects.ForEachInGroup(
            GroupFilter.AllCreatures,
            Effects.DealDamage(1, EffectTarget.IterationEntity)
        )
        kickerEffect = Effects.ForEachInGroup(
            GroupFilter.AllCreatures,
            Effects.DealDamage(2, EffectTarget.IterationEntity)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "136"
        artist = "Campbell White"
        flavorText = "Every inferno begins with a spark."
        imageUri = "https://cards.scryfall.io/normal/front/5/5/5516cf97-805f-4a21-a4c6-2d6e55865336.jpg?1783929361"
    }
}
