package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Moonfolk Puzzlemaker
 * {2}{U}
 * Artifact Creature — Moonfolk Wizard
 * 1/4
 * Flying
 * Whenever this creature becomes tapped, scry 1.
 */
val MoonfolkPuzzlemaker = card("Moonfolk Puzzlemaker") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Artifact Creature — Moonfolk Wizard"
    oracleText = "Flying\nWhenever this creature becomes tapped, scry 1."
    power = 1
    toughness = 4

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.becomesTapped()
        effect = Patterns.Library.scry(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "68"
        artist = "Miguel Mercado"
        flavorText = "\"Research is hardly the dry, serious pastime outsiders make it out to be.\""
        imageUri = "https://cards.scryfall.io/normal/front/c/9/c9fa6d4c-78dc-4b40-9ce8-a0e593d4001f.jpg"
    }
}
