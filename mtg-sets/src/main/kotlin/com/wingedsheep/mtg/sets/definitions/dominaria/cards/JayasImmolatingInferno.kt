package com.wingedsheep.mtg.sets.definitions.dominaria.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ForEachTargetEffect
import com.wingedsheep.sdk.scripting.targets.AnyTarget
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Jaya's Immolating Inferno
 * {X}{R}{R}
 * Legendary Sorcery
 * (You may cast a legendary sorcery only if you control a legendary creature or planeswalker.)
 * Jaya's Immolating Inferno deals X damage to each of up to three targets.
 */
val JayasImmolatingInferno = card("Jaya's Immolating Inferno") {
    manaCost = "{X}{R}{R}"
    typeLine = "Legendary Sorcery"
    oracleText = "(You may cast a legendary sorcery only if you control a legendary creature or planeswalker.)\nJaya's Immolating Inferno deals X damage to each of up to three targets."

    spell {
        target = AnyTarget(count = 3, minCount = 1)
        effect = ForEachTargetEffect(listOf(
            Effects.DealXDamage(EffectTarget.ContextTarget(0))
        ))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "133"
        artist = "Noah Bradley"
        flavorText = "Centuries ago, a pyromancer's spark ignited a fiery conflagration."
        imageUri = "https://cards.scryfall.io/normal/front/4/8/48268bb8-1326-451b-9fae-de6605eb6cfd.jpg?1562735063"
    }
}
