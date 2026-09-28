package com.wingedsheep.mtg.sets.definitions.p02.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Wind Sail
 * {1}{U}
 * Sorcery
 *
 * One or two target creatures gain flying until end of turn.
 */
val WindSail = card("Wind Sail") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "One or two target creatures gain flying until end of turn."

    spell {
        target = TargetObject(filter = TargetFilter(GameObjectFilter.Creature), count = 2, minCount = 1)
        effect = Effects.ForEachTarget(
            Effects.GrantKeyword(Keyword.FLYING, EffectTarget.ContextTarget(0)),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "60"
        artist = "Matt Stawicki"
        flavorText = "It pays to be at home both on the sea *and* in the sky."
        imageUri = "https://cards.scryfall.io/normal/front/1/7/17ef39fd-96d0-4a02-97d5-580d121b92a3.jpg?1783946480"
    }
}
