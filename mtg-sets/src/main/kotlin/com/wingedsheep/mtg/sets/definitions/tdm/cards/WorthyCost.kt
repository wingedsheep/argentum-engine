package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreatureOrPlaneswalker
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Costs

/**
 * Worthy Cost — Tarkir: Dragonstorm #99
 * {B} · Sorcery
 *
 * As an additional cost to cast this spell, sacrifice a creature.
 * Exile target creature or planeswalker.
 */
val WorthyCost = card("Worthy Cost") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "As an additional cost to cast this spell, sacrifice a creature.\nExile target creature or planeswalker."

    additionalCost(Costs.additional.SacrificePermanent(GameObjectFilter.Creature))

    spell {
        val t = target("target", TargetCreatureOrPlaneswalker())
        effect = Effects.Move(t, Zone.EXILE)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "99"
        artist = "Andrew Mar"
        flavorText = "\"'Victory is survival' is the tenet that guides us. My love, know that I will always ensure your survivial.\"\n—Duvir, Mardu warrior, final letter"
        imageUri = "https://cards.scryfall.io/normal/front/a/d/adc18edc-01d8-4a7e-a87b-a854e50aa75e.jpg?1743204359"
    }
}
