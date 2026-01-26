package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.DealDamageEffect
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.GainLifeEffect
import com.wingedsheep.sdk.targeting.AnyTarget

/**
 * Vampiric Feast
 * {5}{B}{B}
 * Sorcery
 * Vampiric Feast deals 4 damage to any target and you gain 4 life.
 */
val VampiricFeast = card("Vampiric Feast") {
    manaCost = "{5}{B}{B}"
    typeLine = "Sorcery"

    spell {
        target = AnyTarget()
        effect = DealDamageEffect(4, EffectTarget.ContextTarget(0)) then
                GainLifeEffect(4, EffectTarget.Controller)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "114"
        artist = "Pete Venters"
        flavorText = "The vampire's hunger knows no bounds."
        imageUri = "https://cards.scryfall.io/normal/front/1/9/19500ffb-bfad-46d6-8a6e-d134405959c0.jpg"
    }
}
