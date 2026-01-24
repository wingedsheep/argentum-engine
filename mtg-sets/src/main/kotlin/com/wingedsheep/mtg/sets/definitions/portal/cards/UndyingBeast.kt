package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.PutOnTopOfLibraryEffect

/**
 * Undying Beast
 * {3}{B}
 * Creature — Beast
 * 3/2
 * When Undying Beast dies, put it on top of its owner's library.
 */
val UndyingBeast = card("Undying Beast") {
    manaCost = "{3}{B}"
    typeLine = "Creature — Beast"
    power = 3
    toughness = 2

    triggeredAbility {
        trigger = Triggers.Dies
        effect = PutOnTopOfLibraryEffect(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "113"
        artist = "Scott Kirschner"
        flavorText = "It returns, again and again, never truly vanquished."
        imageUri = "https://cards.scryfall.io/normal/front/3/a/3a0b1c2d-4e5f-6a7b-8c9d-0e1f2a3b4c5d.jpg"
    }
}
