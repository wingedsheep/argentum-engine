package com.wingedsheep.mtg.sets.definitions.legions.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Shaleskin Plower
 * {3}{R}
 * Creature — Beast
 * 3/2
 * Morph {4}{R} (You may cast this card face down as a 2/2 creature for {3}. Turn it face up any time for its morph cost.)
 * When this creature is turned face up, destroy target land.
 */
val ShaleskinPlower = card("Shaleskin Plower") {
    manaCost = "{3}{R}"
    typeLine = "Creature — Beast"
    power = 3
    toughness = 2
    oracleText = "Morph {4}{R} (You may cast this card face down as a 2/2 creature for {3}. Turn it face up any time for its morph cost.)\nWhen this creature is turned face up, destroy target land."

    triggeredAbility {
        trigger = Triggers.TurnedFaceUp
        val t = target("land", Targets.Land)
        effect = Effects.Destroy(t)
    }

    morph = "{4}{R}"

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "110"
        artist = "Daren Bader"
        imageUri = "https://cards.scryfall.io/normal/front/4/2/42658b33-9a12-403b-bc7d-807fbe1f1a36.jpg?1562908348"
    }
}
