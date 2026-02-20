package com.wingedsheep.mtg.sets.definitions.scourge.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.triggers.OnTurnFaceUp

/**
 * Aven Farseer
 * {1}{W}
 * Creature — Bird Soldier
 * 1/1
 * Flying
 * Whenever a permanent is turned face up, Aven Farseer gets +2/+2 until end of turn.
 */
val AvenFarseer = card("Aven Farseer") {
    manaCost = "{1}{W}"
    typeLine = "Creature — Bird Soldier"
    power = 1
    toughness = 1
    oracleText = "Flying\nWhenever a permanent is turned face up, Aven Farseer gets +2/+2 until end of turn."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = OnTurnFaceUp(selfOnly = false)
        effect = Effects.ModifyStats(2, 2, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "2"
        artist = "Matthew D. Wilson"
        flavorText = "Those trained in the ways of the morph serve as the Order's spies and scouts."
        imageUri = "https://cards.scryfall.io/large/front/4/7/47854e89-4d22-4eb6-a77d-6f04407bd2e5.jpg?1562528498"
    }
}
