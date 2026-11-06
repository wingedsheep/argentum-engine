package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Revolutionary Rebuff
 * {1}{U}
 * Instant
 * Counter target nonartifact spell unless its controller pays {2}.
 */
val RevolutionaryRebuff = card("Revolutionary Rebuff") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Counter target nonartifact spell unless its controller pays {2}."

    spell {
        target(TargetFilter(GameObjectFilter.Nonartifact, zone = Zone.STACK))
        effect = Effects.CounterUnlessPays("{2}")
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "61"
        artist = "Zack Stella"
        flavorText = "By the time a Consulate sanction becomes official, the renegade quicksmiths have already found a way to circumvent it."
        imageUri = "https://cards.scryfall.io/normal/front/6/e/6ea63dad-6afe-464e-ab19-fabd9709c6f9.jpg?1783937215"
    }
}
