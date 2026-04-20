package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Reckless Ransacking
 * {1}{R}
 * Instant
 *
 * Target creature gets +3/+2 until end of turn. Create a Treasure token.
 */
val RecklessRansacking = card("Reckless Ransacking") {
    manaCost = "{1}{R}"
    typeLine = "Instant"
    oracleText = "Target creature gets +3/+2 until end of turn. Create a Treasure token. (It's an artifact with \"{T}, Sacrifice this token: Add one mana of any color.\")"

    spell {
        val creature = target("creature", Targets.Creature)
        effect = Effects.ModifyStats(3, 2, EffectTarget.ContextTarget(0))
            .then(Effects.CreateTreasure())
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "152"
        artist = "Daren Bader"
        flavorText = "Most thieves are silent and swift, leaving no trace of their crime. Noggles take a different approach."
        imageUri = "https://cards.scryfall.io/normal/front/2/4/24a5b025-4cdb-416d-aad6-0fc7e8da3df2.jpg?1767957141"
    }
}
