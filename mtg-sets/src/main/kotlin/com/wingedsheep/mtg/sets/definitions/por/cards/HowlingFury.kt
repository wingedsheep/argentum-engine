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
 * Howling Fury
 * {2}{B}
 * Sorcery
 * Target creature gets +4/+0 until end of turn.
 */
val HowlingFury = card("Howling Fury") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    spell {
        val t = target("target", TargetCreature(filter = TargetFilter.Creature))
        effect = ModifyStatsEffect(powerModifier = 4, toughnessModifier = 0, target = t)
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "97"
        artist = "Mike Dringenberg"
        flavorText = "I howl my soul to the moon, and the moon howls with me."
        imageUri = "https://cards.scryfall.io/normal/front/a/4/a49a7c61-8696-4bab-9c96-05028db3a9f9.jpg"
    }
}
