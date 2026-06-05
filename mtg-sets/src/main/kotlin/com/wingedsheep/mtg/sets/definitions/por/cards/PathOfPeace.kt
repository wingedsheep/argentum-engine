// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.effects.OwnerGainsLifeEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature


/**
 * Path of Peace
 * {3}{W}
 * Sorcery
 * Destroy target creature. Its owner gains 4 life.
 */
val PathofPeace = card("Path of Peace") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    spell {
        val t = target("target", TargetCreature(filter = TargetFilter.Creature))
        effect = CompositeEffect(
        listOf(
            MoveToZoneEffect(t, Zone.GRAVEYARD, byDestruction = true),
            OwnerGainsLifeEffect(4)
        )
    )
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "21"
        artist = "Pete Venters"
        flavorText = "The soldier reaped the profits of peace."
        imageUri = "https://cards.scryfall.io/normal/front/a/1/a1f3e1c9-bfad-49a1-b171-6fa344ef2eef.jpg"
    }
}
