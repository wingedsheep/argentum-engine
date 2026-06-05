// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature


/**
 * Tidal Surge
 * {1}{U}
 * Sorcery
 * Tap up to three target creatures without flying.
 */
val TidalSurge = card("Tidal Surge") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    spell {
        val t = target("target", TargetCreature(optional = true, count = 3, filter = TargetFilter.Creature.withoutKeyword(Keyword.FLYING)))
        effect = Effects.TapEachTarget()
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "74"
        artist = "Douglas Shuler"
        flavorText = "\"'Twas when the seas were roaring With hollow blasts of wind . . . .\"\n—John Gay, \"The What D'Ye Call It\""
        imageUri = "https://cards.scryfall.io/normal/front/a/0/a027c31d-c662-4ce1-a0d1-a32e62f6a724.jpg"
    }
}
