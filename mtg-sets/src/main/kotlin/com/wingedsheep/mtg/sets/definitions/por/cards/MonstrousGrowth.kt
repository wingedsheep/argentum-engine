// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ModifyStatsEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature


/**
 * Monstrous Growth
 * {1}{G}
 * Sorcery
 * Target creature gets +4/+4 until end of turn.
 */
val MonstrousGrowth = card("Monstrous Growth") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    spell {
        val t = target("target", TargetCreature(filter = TargetFilter.Creature))
        effect = ModifyStatsEffect(powerModifier = 4, toughnessModifier = 4, target = t)
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "173"
        artist = "Dan Frazier"
        flavorText = "Some cats are born fighters; others need a little persuading."
        imageUri = "https://cards.scryfall.io/normal/front/1/f/1fd2edb9-0b53-432e-bb3b-171d2a85439d.jpg"
    }
}
