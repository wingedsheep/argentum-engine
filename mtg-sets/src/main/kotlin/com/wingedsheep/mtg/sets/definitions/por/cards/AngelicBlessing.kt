// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.GrantKeywordEffect
import com.wingedsheep.sdk.scripting.effects.ModifyStatsEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature


/**
 * Angelic Blessing
 * {2}{W}
 * Sorcery
 * Target creature gets +3/+3 and gains flying until end of turn. (It can't be blocked except by creatures with flying or reach.)
 */
val AngelicBlessing = card("Angelic Blessing") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    spell {
        val t = target("target", TargetCreature(filter = TargetFilter.Creature))
        effect = CompositeEffect(listOf(ModifyStatsEffect(powerModifier = 3, toughnessModifier = 3, target = t), GrantKeywordEffect(Keyword.FLYING, t)))
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "2"
        artist = "DiTerlizzi"
        flavorText = "A peasant can do more by faith than a king by proclamation."
        imageUri = "https://cards.scryfall.io/normal/front/3/1/31dda640-2a00-437e-855f-173c487e7395.jpg"
    }
}
