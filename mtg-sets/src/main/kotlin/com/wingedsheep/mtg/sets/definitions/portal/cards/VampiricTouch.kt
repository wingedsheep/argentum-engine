package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.effects.GainLifeEffect
import com.wingedsheep.sdk.scripting.targets.TargetOpponent

/**
 * Vampiric Touch
 * {2}{B}
 * Sorcery
 * Vampiric Touch deals 2 damage to target opponent and you gain 2 life.
 */
val VampiricTouch = card("Vampiric Touch") {
    manaCost = "{2}{B}"
    typeLine = "Sorcery"

    spell {
        target = TargetOpponent()
        effect = DealDamageEffect(2, EffectTarget.ContextTarget(0)) then
                GainLifeEffect(2, EffectTarget.Controller)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "115"
        artist = "Mike Raabe"
        flavorText = "A gentle caress that drains the soul."
        imageUri = "https://cards.scryfall.io/normal/front/2/3/231f7598-8c47-4828-8240-e2a545a7ac5b.jpg"
    }
}
