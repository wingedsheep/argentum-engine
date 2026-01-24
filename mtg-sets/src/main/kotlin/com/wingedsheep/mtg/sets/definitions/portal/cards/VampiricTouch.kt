package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.DealDamageEffect
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.GainLifeEffect
import com.wingedsheep.sdk.targeting.TargetOpponent

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
        imageUri = "https://cards.scryfall.io/normal/front/5/c/5c0d1e2f-6a7b-8c9d-0e1f-2a3b4c5d6e7f.jpg"
    }
}
