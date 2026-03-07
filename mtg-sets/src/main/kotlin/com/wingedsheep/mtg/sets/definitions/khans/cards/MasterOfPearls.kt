package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Master of Pearls
 * {1}{W}
 * Creature — Human Monk
 * 2/2
 * Morph {3}{W}{W} (You may cast this card face down as a 2/2 creature for {3}. Turn it face up any time for its morph cost.)
 * When this creature is turned face up, creatures you control get +2/+2 until end of turn.
 */
val MasterOfPearls = card("Master of Pearls") {
    manaCost = "{1}{W}"
    typeLine = "Creature — Human Monk"
    power = 2
    toughness = 2
    oracleText = "Morph {3}{W}{W} (You may cast this card face down as a 2/2 creature for {3}. Turn it face up any time for its morph cost.)\nWhen Master of Pearls is turned face up, creatures you control get +2/+2 until end of turn."

    morph = "{3}{W}{W}"

    triggeredAbility {
        trigger = Triggers.TurnedFaceUp
        effect = EffectPatterns.modifyStatsForAll(2, 2, GroupFilter.AllCreaturesYouControl)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "18"
        artist = "David Gaillet"
        imageUri = "https://cards.scryfall.io/normal/front/e/8/e8455229-6fec-4843-b888-e701c09620af.jpg?1562795295"
    }
}
