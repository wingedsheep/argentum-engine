package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.targets.TargetOther

/**
 * Cosmic Hunger
 * {1}{G}
 * Instant
 * Target creature you control deals damage equal to its power to another target creature,
 * planeswalker, or battle.
 *
 * The Markov Retribution bite idiom: the biter is target 0, the victim is a [TargetOther]
 * creature/planeswalker/battle (which may be one you control), damage equals the biter's power
 * read at resolution, and the biter is the damage source.
 */
val CosmicHunger = card("Cosmic Hunger") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Target creature you control deals damage equal to its power to another target " +
        "creature, planeswalker, or battle."

    spell {
        val biter = target(TargetFilter.Creature.youControl())
        val victim = target(TargetOther(TargetObject(filter = TargetFilter.CreaturePlaneswalkerOrBattle)))
        effect = Effects.DealDamage(DynamicAmounts.powerOf(biter), victim, damageSource = biter)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "182"
        artist = "Konstantin Porubov"
        flavorText = "The Copper Host sought only the strongest converts. In Koma, it found perfection."
        imageUri = "https://cards.scryfall.io/normal/front/b/8/b8eef541-6851-4312-ad2b-74f45c7ede6c.jpg?1783916972"
    }
}
