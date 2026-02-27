package com.wingedsheep.mtg.sets.definitions.legions.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Patron of the Wild
 * {G}
 * Creature — Elf
 * 1/1
 * Morph {2}{G}
 * When this creature is turned face up, target creature gets +3/+3 until end of turn.
 */
val PatronOfTheWild = card("Patron of the Wild") {
    manaCost = "{G}"
    typeLine = "Creature — Elf"
    power = 1
    toughness = 1
    oracleText = "Morph {2}{G} (You may cast this card face down as a 2/2 creature for {3}. Turn it face up any time for its morph cost.)\nWhen this creature is turned face up, target creature gets +3/+3 until end of turn."

    triggeredAbility {
        trigger = Triggers.TurnedFaceUp
        val t = target("creature", Targets.Creature)
        effect = Effects.ModifyStats(3, 3, t)
    }

    morph = "{2}{G}"

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "134"
        artist = "Dave Dorman"
        imageUri = "https://cards.scryfall.io/normal/front/7/f/7f7a0810-3970-454f-8381-700d6c6aefdc.jpg?1562920729"
    }
}
