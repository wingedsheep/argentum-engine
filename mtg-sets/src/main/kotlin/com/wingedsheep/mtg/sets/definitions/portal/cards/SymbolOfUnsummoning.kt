package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CompositeEffect
import com.wingedsheep.sdk.scripting.DrawCardsEffect
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.MoveToZoneEffect
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.targeting.TargetCreature

/**
 * Symbol of Unsummoning
 * {2}{U}
 * Sorcery
 * Return target creature to its owner's hand. Draw a card.
 */
val SymbolOfUnsummoning = card("Symbol of Unsummoning") {
    manaCost = "{2}{U}"
    typeLine = "Sorcery"

    spell {
        target = TargetCreature()
        effect = CompositeEffect(
            listOf(
                MoveToZoneEffect(EffectTarget.ContextTarget(0), Zone.HAND),
                DrawCardsEffect(1)
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "70"
        artist = "Adam Rex"
        imageUri = "https://cards.scryfall.io/normal/front/5/5/55811106-9f30-4e34-924e-2c9401b49574.jpg"
    }
}
