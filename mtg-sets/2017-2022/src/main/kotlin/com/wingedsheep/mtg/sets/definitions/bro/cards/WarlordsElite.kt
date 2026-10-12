package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Warlord's Elite
 * {2}{W}
 * Creature — Human Soldier
 * 4/4
 * As an additional cost to cast this spell, tap two untapped artifacts, creatures, and/or lands
 * you control.
 *
 * Same shape as Guardian of the Great Door: AdditionalCost.TapPermanents over an
 * artifact-or-creature-or-land filter, paid as the spell is cast. The cost atom enforces
 * "untapped you control" itself.
 */
val WarlordsElite = card("Warlord's Elite") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Soldier"
    oracleText = "As an additional cost to cast this spell, tap two untapped artifacts, creatures, " +
        "and/or lands you control."
    power = 4
    toughness = 4

    additionalCost(
        Costs.additional.TapPermanents(
            count = 2,
            filter = GameObjectFilter.Artifact or GameObjectFilter.Creature or GameObjectFilter.Land
        )
    )

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "32"
        artist = "Joshua Raphael"
        flavorText = "\"The Warlord thinks we've gone soft, does he? The Fist of Progress will prove him wrong!\""
        imageUri = "https://cards.scryfall.io/normal/front/1/8/1873cc9e-30b6-498e-b008-9d11a8bbf9b9.jpg?1783920122"
    }
}
