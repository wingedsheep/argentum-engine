package com.wingedsheep.mtg.sets.definitions.m13.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Liliana's Shade
 * {2}{B}{B}
 * Creature — Shade
 * 1/1
 * When this creature enters, you may search your library for a Swamp card, reveal it, put it into
 * your hand, then shuffle.
 * {B}: This creature gets +1/+1 until end of turn.
 */
val LilianasShade = card("Liliana's Shade") {
    manaCost = "{2}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Shade"
    oracleText = "When this creature enters, you may search your library for a Swamp card, reveal it, put it into your hand, then shuffle.\n" +
        "{B}: This creature gets +1/+1 until end of turn."
    power = 1
    toughness = 1

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.May(
            Patterns.Library.searchLibrary(
                filter = GameObjectFilter.Land.withSubtype("Swamp"),
                destination = SearchDestination.HAND,
                reveal = true
            )
        )
    }

    activatedAbility {
        cost = Costs.Mana("{B}")
        effect = Effects.ModifyStats(1, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "98"
        artist = "Eric Deschamps"
        imageUri = "https://cards.scryfall.io/normal/front/1/c/1cf0c01d-a4a0-43fb-970d-e428e9ac63d7.jpg"
    }
}
