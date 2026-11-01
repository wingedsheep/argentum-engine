package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Forced Landing
 * {1}{G}
 * Instant
 * Put target creature with flying on the bottom of its owner's library.
 */
val ForcedLanding = card("Forced Landing") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Put target creature with flying on the bottom of its owner's library."

    spell {
        val t = target(TargetFilter.Creature.withKeyword(Keyword.FLYING))
        effect = Effects.PutOnBottomOfLibrary(t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "161"
        artist = "Svetlin Velinov"
        flavorText = "The druid knew only one spell, a charm to encourage rapid plant growth. When airborne Eternals invaded her arboretum, she had to get creative."
        imageUri = "https://cards.scryfall.io/normal/front/5/c/5cb319a7-564c-4748-82cf-c26ab110c32c.jpg?1783933414"
    }
}
