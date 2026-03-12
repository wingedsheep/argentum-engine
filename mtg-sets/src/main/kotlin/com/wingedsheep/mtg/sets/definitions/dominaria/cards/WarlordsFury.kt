package com.wingedsheep.mtg.sets.definitions.dominaria.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Warlord's Fury
 * {R}
 * Sorcery
 * Creatures you control gain first strike until end of turn.
 * Draw a card.
 */
val WarlordsFury = card("Warlord's Fury") {
    manaCost = "{R}"
    typeLine = "Sorcery"
    oracleText = "Creatures you control gain first strike until end of turn.\nDraw a card."

    spell {
        effect = EffectPatterns.grantKeywordToAll(
            Keyword.FIRST_STRIKE,
            GroupFilter(GameObjectFilter.Creature.youControl())
        ) then Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "151"
        artist = "Volkan Baǵa"
        flavorText = "\"The old ways were for a winter world. We do what's best for us, not for the past. We are Keldons!\" —Grand Warlord Radha"
        imageUri = "https://cards.scryfall.io/normal/front/0/e/0ebd63cf-7e8c-4c8d-844d-98535d5f3039.jpg?1562731381"
    }
}
