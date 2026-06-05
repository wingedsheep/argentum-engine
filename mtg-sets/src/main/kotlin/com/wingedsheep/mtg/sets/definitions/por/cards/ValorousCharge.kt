// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ForEachInGroupEffect
import com.wingedsheep.sdk.scripting.effects.ModifyStatsEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget


/**
 * Valorous Charge
 * {1}{W}{W}
 * Sorcery
 * White creatures get +2/+0 until end of turn.
 */
val ValorousCharge = card("Valorous Charge") {
    manaCost = "{1}{W}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    spell {
        effect = ForEachInGroupEffect(GroupFilter(GameObjectFilter.Creature.withColor(Color.WHITE)), ModifyStatsEffect(powerModifier = 2, toughnessModifier = 0, target = EffectTarget.Self))
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "34"
        artist = "Douglas Shuler"
        flavorText = "Stand in the way of truth at your peril."
        imageUri = "https://cards.scryfall.io/normal/front/6/7/67f61bdf-cbcd-4a63-8866-eb13ec9b351c.jpg"
    }
}
