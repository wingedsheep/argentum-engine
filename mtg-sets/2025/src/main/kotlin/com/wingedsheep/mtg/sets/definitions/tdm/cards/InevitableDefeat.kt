package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Inevitable Defeat
 * {1}{R}{W}{B}
 * Instant
 * This spell can't be countered.
 * Exile target nonland permanent. Its controller loses 3 life and you gain 3 life.
 */
val InevitableDefeat = card("Inevitable Defeat") {
    manaCost = "{1}{R}{W}{B}"
    colorIdentity = "WBR"
    typeLine = "Instant"
    cantBeCountered = true
    oracleText = "This spell can't be countered.\n" +
        "Exile target nonland permanent. Its controller loses 3 life and you gain 3 life."

    spell {
        val permanent = target(TargetFilter.NonlandPermanent)
        effect = Effects.Exile(permanent) then
            Effects.LoseLife(3, EffectTarget.TargetController) then
            Effects.GainLife(3, EffectTarget.Controller)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "194"
        artist = "Cristi Balanescu"
        imageUri = "https://cards.scryfall.io/normal/front/9/d/9d677980-b608-407e-9f17-790a81263f15.jpg?1743204760"
    }
}
