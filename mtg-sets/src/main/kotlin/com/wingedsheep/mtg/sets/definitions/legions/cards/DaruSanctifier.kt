package com.wingedsheep.mtg.sets.definitions.legions.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Daru Sanctifier
 * {3}{W}
 * Creature — Human Cleric
 * 1/4
 * Morph {1}{W} (You may cast this card face down as a 2/2 creature for {3}. Turn it face up any time for its morph cost.)
 * When Daru Sanctifier is turned face up, destroy target enchantment.
 */
val DaruSanctifier = card("Daru Sanctifier") {
    manaCost = "{3}{W}"
    typeLine = "Creature — Human Cleric"
    power = 1
    toughness = 4
    oracleText = "Morph {1}{W} (You may cast this card face down as a 2/2 creature for {3}. Turn it face up any time for its morph cost.)\nWhen Daru Sanctifier is turned face up, destroy target enchantment."

    triggeredAbility {
        trigger = Triggers.TurnedFaceUp
        val t = target("enchantment", Targets.Enchantment)
        effect = Effects.Destroy(t)
    }

    morph = "{1}{W}"

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "9"
        artist = "Tony Szczudlo"
        imageUri = "https://cards.scryfall.io/normal/front/3/8/38b14a24-c74a-4465-9b36-8f5309e0a333.jpg?1562906416"
    }
}
