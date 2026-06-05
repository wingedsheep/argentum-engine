// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.effects.ForEachInGroupEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget


/**
 * Needle Storm
 * {2}{G}
 * Sorcery
 * Needle Storm deals 4 damage to each creature with flying.
 */
val NeedleStorm = card("Needle Storm") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    spell {
        effect = ForEachInGroupEffect(GroupFilter(GameObjectFilter.Creature.withKeyword(Keyword.FLYING)), DealDamageEffect(4, EffectTarget.Self))
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "179"
        artist = "Charles Gillespie"
        imageUri = "https://cards.scryfall.io/normal/front/2/9/29a44e44-94b1-4bd2-8e00-6bd2ec07ee4c.jpg"
    }
}
