// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ForEachInGroupEffect
import com.wingedsheep.sdk.scripting.effects.ModifyStatsEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget


/**
 * Warrior's Charge
 * {2}{W}
 * Sorcery
 * Creatures you control get +1/+1 until end of turn.
 */
val WarriorsCharge = card("Warrior's Charge") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    spell {
        effect = ForEachInGroupEffect(GroupFilter(GameObjectFilter.Creature.youControl()), ModifyStatsEffect(powerModifier = 1, toughnessModifier = 1, target = EffectTarget.Self))
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "38"
        artist = "Ted Naifeh"
        flavorText = "It is not the absence of fear that makes a warrior, but its domination."
        imageUri = "https://cards.scryfall.io/normal/front/8/6/8668e4af-ae89-4fab-8015-8dc643c6cd36.jpg"
    }
}
