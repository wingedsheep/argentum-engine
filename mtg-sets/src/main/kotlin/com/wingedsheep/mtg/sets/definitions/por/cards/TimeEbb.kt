package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import com.wingedsheep.sdk.dsl.Effects

/**
 * Time Ebb
 * {2}{U}
 * Sorcery
 * Put target creature on top of its owner's library.
 */
val TimeEbb = card("Time Ebb") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"

    spell {
        target = TargetCreature()
        effect = Effects.Move(EffectTarget.ContextTarget(0), Zone.LIBRARY, ZonePlacement.Top)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "75"
        artist = "Randy Gallegos"
        imageUri = "https://cards.scryfall.io/normal/front/e/5/e5fd26ca-dc7d-453d-8653-7f967e8f6dc7.jpg"
    }
}
