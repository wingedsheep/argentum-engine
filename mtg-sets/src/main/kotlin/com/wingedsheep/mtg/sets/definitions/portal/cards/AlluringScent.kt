package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.effects.MustBeBlockedEffect
import com.wingedsheep.sdk.scripting.targets.TargetCreature

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
        imageUri = "https://cards.scryfall.io/normal/front/8/7/8726242e-bfd8-4ed5-a016-ac0c82e4762b.jpg"
    }
}
