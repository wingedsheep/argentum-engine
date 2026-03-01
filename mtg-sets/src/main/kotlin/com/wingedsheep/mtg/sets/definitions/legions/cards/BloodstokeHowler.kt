package com.wingedsheep.mtg.sets.definitions.legions.cards

import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ForEachInGroupEffect
import com.wingedsheep.sdk.scripting.effects.ModifyStatsEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Bloodstoke Howler
 * {5}{R}
 * Creature — Beast
 * 3/4
 * Morph {6}{R}
 * When this creature is turned face up, Beast creatures you control get +3/+0 until end of turn.
 */
val BloodstokeHowler = card("Bloodstoke Howler") {
    manaCost = "{5}{R}"
    typeLine = "Creature — Beast"
    power = 3
    toughness = 4
    oracleText = "Morph {6}{R} (You may cast this card face down as a 2/2 creature for {3}. Turn it face up any time for its morph cost.)\nWhen this creature is turned face up, Beast creatures you control get +3/+0 until end of turn."

    triggeredAbility {
        trigger = Triggers.TurnedFaceUp
        effect = ForEachInGroupEffect(
            filter = GroupFilter.allCreaturesWithSubtype("Beast").youControl(),
            effect = ModifyStatsEffect(3, 0, EffectTarget.Self)
        )
    }

    morph = "{6}{R}"

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "89"
        artist = "Christopher Moeller"
        imageUri = "https://cards.scryfall.io/normal/front/7/4/743779d4-fee8-4b8d-a5ac-27f355e006e5.jpg?1562918274"
    }
}
