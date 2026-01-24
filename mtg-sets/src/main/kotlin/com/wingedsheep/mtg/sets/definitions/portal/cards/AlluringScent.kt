package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.MustBeBlockedEffect
import com.wingedsheep.sdk.targeting.TargetCreature

/**
 * Alluring Scent
 * {1}{G}{G}
 * Sorcery
 * All creatures able to block target creature this turn do so.
 */
val AlluringScent = card("Alluring Scent") {
    manaCost = "{1}{G}{G}"
    typeLine = "Sorcery"

    spell {
        target = TargetCreature()
        effect = MustBeBlockedEffect(EffectTarget.ContextTarget(0))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "157"
        artist = "Heather Hudson"
        flavorText = "None can resist the call."
        imageUri = "https://cards.scryfall.io/normal/front/4/b/4bc6f7a8-c9d0-e1f2-a3b4-c5d6e7f8a9b0.jpg"
    }
}
