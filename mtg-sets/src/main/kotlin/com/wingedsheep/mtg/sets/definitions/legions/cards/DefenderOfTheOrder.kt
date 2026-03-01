package com.wingedsheep.mtg.sets.definitions.legions.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Defender of the Order
 * {3}{W}
 * Creature — Human Cleric
 * 2/4
 * Morph {W}{W} (You may cast this card face down as a 2/2 creature for {3}. Turn it face up any time for its morph cost.)
 * When this creature is turned face up, creatures you control get +0/+2 until end of turn.
 */
val DefenderOfTheOrder = card("Defender of the Order") {
    manaCost = "{3}{W}"
    typeLine = "Creature — Human Cleric"
    power = 2
    toughness = 4
    oracleText = "Morph {W}{W} (You may cast this card face down as a 2/2 creature for {3}. Turn it face up any time for its morph cost.)\nWhen this creature is turned face up, creatures you control get +0/+2 until end of turn."

    triggeredAbility {
        trigger = Triggers.TurnedFaceUp
        effect = Effects.ModifyStatsForAll(0, 2, GroupFilter.AllCreaturesYouControl)
    }

    morph = "{W}{W}"

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "11"
        artist = "Darrell Riche"
        imageUri = "https://cards.scryfall.io/normal/front/2/3/236b1c88-20a0-479e-91fb-16bb77f699fe.jpg?1562902106"
    }
}
