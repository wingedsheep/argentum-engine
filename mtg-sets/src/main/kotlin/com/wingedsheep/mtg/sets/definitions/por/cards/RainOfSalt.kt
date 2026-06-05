// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ForEachTargetEffect
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetPermanent


/**
 * Rain of Salt
 * {4}{R}{R}
 * Sorcery
 * Destroy two target lands.
 */
val RainofSalt = card("Rain of Salt") {
    manaCost = "{4}{R}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    spell {
        val t = target("target", TargetPermanent(count = 2, filter = TargetFilter.Land))
        effect = ForEachTargetEffect(listOf(MoveToZoneEffect(EffectTarget.ContextTarget(0), Zone.GRAVEYARD, byDestruction = true)))
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "147"
        artist = "Charles Gillespie"
        flavorText = "It pelts the land with its fury, spoiling what was once sweet."
        imageUri = "https://cards.scryfall.io/normal/front/6/6/661ffab2-9cf5-492d-874f-de73d7a13e2b.jpg"
    }
}
