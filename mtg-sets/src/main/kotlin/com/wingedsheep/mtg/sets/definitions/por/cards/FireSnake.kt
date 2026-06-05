// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent


/**
 * Fire Snake
 * {4}{R}
 * Creature — Snake
 * 3/1
 * When this creature dies, destroy target land.
 */
val FireSnake = card("Fire Snake") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Snake"
    power = 3
    toughness = 1
    triggeredAbility {
        trigger = Triggers.Dies
        val t = target("target", TargetPermanent(filter = TargetFilter.Land))
        effect = MoveToZoneEffect(t, Zone.GRAVEYARD, byDestruction = true)
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "127"
        artist = "Steve Luke"
        flavorText = "The snake's final thrashings only spread the fire within it."
        imageUri = "https://cards.scryfall.io/normal/front/d/4/d4c36e32-59e8-4e3d-903e-a264211f2a82.jpg"
    }
}
