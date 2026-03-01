package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.NotCondition
import com.wingedsheep.sdk.scripting.conditions.YouAttackedThisTurn
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Bellowing Saddlebrute
 * {3}{B}
 * Creature — Orc Warrior
 * 4/5
 * Raid — When Bellowing Saddlebrute enters the battlefield, you lose 4 life unless you attacked this turn.
 */
val BellowingSaddlebrute = card("Bellowing Saddlebrute") {
    manaCost = "{3}{B}"
    typeLine = "Creature — Orc Warrior"
    power = 4
    toughness = 5
    oracleText = "Raid — When Bellowing Saddlebrute enters, you lose 4 life unless you attacked this turn."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = ConditionalEffect(
            condition = NotCondition(YouAttackedThisTurn),
            effect = Effects.LoseLife(4, EffectTarget.Controller)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "64"
        artist = "Torstein Nordstrand"
        imageUri = "https://cards.scryfall.io/normal/front/a/1/a1939b5d-e24f-4e4b-b4c5-8bdb232d8926.jpg?1562791292"
    }
}
