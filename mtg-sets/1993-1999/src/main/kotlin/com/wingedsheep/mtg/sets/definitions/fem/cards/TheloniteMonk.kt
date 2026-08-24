package com.wingedsheep.mtg.sets.definitions.fem.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Thelonite Monk
 * {2}{G}{G}
 * Creature — Insect Monk Cleric
 * 1/2
 * {T}, Sacrifice a green creature: Target land becomes a Forest.
 *
 * "Becomes a Forest" replaces the land's existing land types (CR 305.7), which is what
 * [Effects.SetLandType] models, and the effect has no duration — [Duration.Permanent] is the
 * "lasts indefinitely" the reminder text calls out.
 */
val TheloniteMonk = card("Thelonite Monk") {
    manaCost = "{2}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Insect Monk Cleric"
    oracleText = "{T}, Sacrifice a green creature: Target land becomes a Forest. (This effect lasts indefinitely.)"
    power = 1
    toughness = 2

    activatedAbility {
        cost = Costs.Composite(
            Costs.Tap,
            Costs.Sacrifice(GameObjectFilter.Creature.withColor(Color.GREEN))
        )
        target = Targets.Land
        effect = Effects.SetLandType(
            landType = "Forest",
            target = EffectTarget.ContextTarget(0),
            duration = Duration.Permanent
        )
        description = "{T}, Sacrifice a green creature: Target land becomes a Forest."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "79"
        artist = "Bryon Wackwitz"
        flavorText = "\"As the climate worsened, some Thelonites turned to fertilizing with fresh blood in an attempt to keep Havenwood alive and growing.\"\n—*Sarpadian Empires, vol. III*"
        imageUri = "https://cards.scryfall.io/normal/front/5/4/5400ff25-c70e-4095-a228-190601b86043.jpg?1783947883"
    }
}
