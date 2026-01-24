package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.DestroyEffect
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.targeting.CreatureTargetFilter
import com.wingedsheep.sdk.targeting.TargetCreature

/**
 * Hand of Death
 * {2}{B}
 * Sorcery
 * Destroy target nonblack creature.
 */
val HandOfDeath = card("Hand of Death") {
    manaCost = "{2}{B}"
    typeLine = "Sorcery"

    spell {
        target = TargetCreature(filter = CreatureTargetFilter.NotColor(Color.BLACK))
        effect = DestroyEffect(EffectTarget.ContextTarget(0))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "96"
        artist = "Brian Snoddy"
        flavorText = "Reach out your hand and touch the dead."
        imageUri = "https://cards.scryfall.io/normal/front/2/0/20e8acd4-5a75-4591-8f51-1e3a8e8e2a3e.jpg"
    }
}
