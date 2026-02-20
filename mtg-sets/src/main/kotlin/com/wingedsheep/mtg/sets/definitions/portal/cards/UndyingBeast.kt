package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.effects.ZonePlacement

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
        effect = MoveToZoneEffect(EffectTarget.Self, Zone.LIBRARY, ZonePlacement.Top)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "113"
        artist = "Scott Kirschner"
        flavorText = "It returns, again and again, never truly vanquished."
        imageUri = "https://cards.scryfall.io/normal/front/9/c/9c95c752-3add-4830-8159-036b8689f40a.jpg"
    }
}
