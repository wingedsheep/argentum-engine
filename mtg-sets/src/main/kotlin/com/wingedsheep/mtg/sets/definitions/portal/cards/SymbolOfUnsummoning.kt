package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CompositeEffect
import com.wingedsheep.sdk.scripting.DrawCardsEffect
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.ReturnToHandEffect
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
                ReturnToHandEffect(EffectTarget.ContextTarget(0)),
                DrawCardsEffect(1)
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "70"
        artist = "Adam Rex"
        imageUri = "https://cards.scryfall.io/normal/front/c/6/c652c8e4-6b4e-4fae-8b68-e3e30efd4193.jpg"
    }
}
