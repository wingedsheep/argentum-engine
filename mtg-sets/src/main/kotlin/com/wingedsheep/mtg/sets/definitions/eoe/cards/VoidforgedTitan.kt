package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Voidforged Titan
 * {4}{B}
 * Artifact Creature — Robot Warrior
 * 5/4
 * Void — At the beginning of your end step, if a nonland permanent left the battlefield this turn
 *   or a spell was warped this turn, you draw a card and lose 1 life.
 */
val VoidforgedTitan = card("Voidforged Titan") {
    manaCost = "{4}{B}"
    colorIdentity = "B"
    typeLine = "Artifact Creature — Robot Warrior"
    power = 5
    toughness = 4
    oracleText = "Void — At the beginning of your end step, if a nonland permanent left the battlefield this turn or a spell was warped this turn, you draw a card and lose 1 life."

    triggeredAbility {
        trigger = Triggers.YourEndStep
        triggerCondition = Conditions.Void
        effect = Effects.DrawCards(1) then Effects.LoseLife(1, EffectTarget.Controller)
        description = "At the beginning of your end step, if a nonland permanent left the battlefield this turn or a spell was warped this turn, you draw a card and lose 1 life."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "125"
        artist = "Diego Gisbert"
        flavorText = "The voidforged lack the formal programming of other mechan. Instead, they are engineered to move in accordance with the hymn of the Immortal Faller."
        imageUri = "https://cards.scryfall.io/normal/front/6/1/6119c016-01b2-44f3-9550-6988324c1d1f.jpg?1752947060"
    }
}
