package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Arborea Pegasus
 * {3}{W}
 * Creature — Pegasus
 * 2/3
 * Flying
 * When this creature enters, target creature gets +1/+1 and gains flying until end of turn.
 */
val ArboreaPegasus = card("Arborea Pegasus") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Pegasus"
    oracleText = "Flying\nWhen this creature enters, target creature gets +1/+1 and gains flying until end of turn."
    power = 2
    toughness = 3

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val t = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(1, 1, t) then Effects.GrantKeyword(Keyword.FLYING, t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "2"
        artist = "Justyna Dura"
        flavorText = "The noble steeds of the elven gods can sometimes be persuaded to allow mere mortals on their backs."
        imageUri = "https://cards.scryfall.io/normal/front/f/c/fc45c9d4-ecc7-4a9d-9efe-f4b7d697dd97.jpg?1783926539"
    }
}
