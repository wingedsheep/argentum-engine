package com.wingedsheep.mtg.sets.definitions.s99.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

val StreamOfAcid = card("Stream of Acid") {
    manaCost = "{2}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Destroy target land or nonblack creature."

    spell {
        val permanent = target(TargetFilter(Filters.Land or Filters.Creature.notColor(Color.BLACK)))
        effect = Effects.Destroy(permanent)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "91"
        artist = "DiTerlizzi"
        imageUri = "https://cards.scryfall.io/normal/front/d/b/dbbf00b3-2a1b-4ad3-8a5b-deec9e08a231.jpg?1783946031"
    }
}
