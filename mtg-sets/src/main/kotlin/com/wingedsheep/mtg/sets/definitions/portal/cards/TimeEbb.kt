package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.PutOnTopOfLibraryEffect
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
        effect = PutOnTopOfLibraryEffect(EffectTarget.ContextTarget(0))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "75"
        artist = "Randy Gallegos"
        imageUri = "https://cards.scryfall.io/normal/front/6/e/6e227c8e-cf40-4e8f-a56d-d30e4adf0f3c.jpg"
    }
}
