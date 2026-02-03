package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.DestroyEffect
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.TargetFilter
import com.wingedsheep.sdk.targeting.TargetCreature

/**
 * Swat
 * {1}{B}{B}
 * Instant
 * Destroy target creature with power 2 or less.
 * Cycling {2}
 */
val Swat = card("Swat") {
    manaCost = "{1}{B}{B}"
    typeLine = "Instant"

    spell {
        target = TargetCreature(unifiedFilter = TargetFilter.Creature.powerAtMost(2))
        effect = DestroyEffect(EffectTarget.ContextTarget(0))
    }

    keywordAbility(KeywordAbility.cycling("{2}"))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "174"
        artist = "rk post"
        flavorText = "Few who cross Phage have the chance to repeat the mistake."
        imageUri = "https://cards.scryfall.io/normal/front/c/e/cec3a260-6c50-401d-a0ff-bf49a973e1a1.jpg?1562943805"
    }
}
