package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.Zone
import com.wingedsheep.sdk.scripting.ZonePlacement
import com.wingedsheep.sdk.targeting.TargetCreature

/**
 * Time Ebb
 * {2}{U}
 * Sorcery
 * Put target creature on top of its owner's library.
 */
val TimeEbb = card("Time Ebb") {
    manaCost = "{2}{U}"
    typeLine = "Sorcery"

    spell {
        target = TargetCreature()
        effect = MoveToZoneEffect(EffectTarget.ContextTarget(0), Zone.Library, ZonePlacement.Top)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "75"
        artist = "Randy Gallegos"
        imageUri = "https://cards.scryfall.io/normal/front/e/5/e5fd26ca-dc7d-453d-8653-7f967e8f6dc7.jpg"
    }
}
