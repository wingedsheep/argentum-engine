package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.PayOrSufferEffect
import com.wingedsheep.sdk.scripting.effects.SacrificeSelfEffect
import com.wingedsheep.sdk.dsl.Costs

/**
 * Thing from the Deep
 * {6}{U}{U}{U}
 * Creature — Leviathan
 * 9/9
 * Whenever Thing from the Deep attacks, sacrifice it unless you sacrifice an Island.
 */
val ThingFromTheDeep = card("Thing from the Deep") {
    manaCost = "{6}{U}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Leviathan"
    power = 9
    toughness = 9

    triggeredAbility {
        trigger = Triggers.Attacks
        effect = PayOrSufferEffect(
            cost = Costs.pay.Sacrifice(GameObjectFilter.Land.withSubtype("Island")),
            suffer = SacrificeSelfEffect
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "73"
        artist = "Pete Venters"
        imageUri = "https://cards.scryfall.io/normal/front/c/b/cb3b9682-7f3a-4857-9ecf-01f3530659fc.jpg"
    }
}
