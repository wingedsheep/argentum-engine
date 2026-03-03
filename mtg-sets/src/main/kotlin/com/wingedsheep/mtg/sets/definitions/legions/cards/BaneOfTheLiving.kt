package com.wingedsheep.mtg.sets.definitions.legions.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Bane of the Living
 * {2}{B}{B}
 * Creature — Insect
 * 4/3
 * Morph {X}{B}{B} (You may cast this card face down as a 2/2 creature for {3}. Turn it face up any time for its morph cost.)
 * When Bane of the Living is turned face up, all creatures get -X/-X until end of turn.
 */
val BaneOfTheLiving = card("Bane of the Living") {
    manaCost = "{2}{B}{B}"
    typeLine = "Creature — Insect"
    oracleText = "Morph {X}{B}{B} (You may cast this card face down as a 2/2 creature for {3}. Turn it face up any time for its morph cost.)\nWhen Bane of the Living is turned face up, all creatures get -X/-X until end of turn."
    power = 4
    toughness = 3

    triggeredAbility {
        trigger = Triggers.TurnedFaceUp
        effect = Effects.ModifyStatsForAll(
            power = DynamicAmount.Multiply(DynamicAmount.XValue, -1),
            toughness = DynamicAmount.Multiply(DynamicAmount.XValue, -1),
            filter = GroupFilter.AllCreatures
        )
    }

    morph = "{X}{B}{B}"

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "60"
        artist = "Justin Sweet"
        imageUri = "https://cards.scryfall.io/normal/front/f/4/f45ebf65-77b8-41bc-b913-d864c4a00549.jpg?1562944144"
        ruling("2004-10-04", "The trigger occurs when you use the Morph ability to turn the card face up, or when an effect turns it face up. It will not trigger on being revealed or on leaving the battlefield.")
        ruling("2004-10-04", "The X in the ability has the same value as the X paid in the Morph ability.")
    }
}
