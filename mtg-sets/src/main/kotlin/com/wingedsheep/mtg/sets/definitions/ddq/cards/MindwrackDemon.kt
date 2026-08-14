package com.wingedsheep.mtg.sets.definitions.ddq.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Mindwrack Demon
 * {2}{B}{B}
 * Creature — Demon
 * 4/5
 * Flying, trample
 * When this creature enters, mill four cards.
 * Delirium — At the beginning of your upkeep, you lose 4 life unless there are four or more
 * card types among cards in your graveyard.
 *
 * Canonical printing is DDQ (pre-SOI).
 */
val MindwrackDemon = card("Mindwrack Demon") {
    manaCost = "{2}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Demon"
    oracleText =
        "Flying, trample\n" +
            "When this creature enters, mill four cards.\n" +
            "Delirium — At the beginning of your upkeep, you lose 4 life unless there are " +
            "four or more card types among cards in your graveyard."
    power = 4
    toughness = 5

    keywords(Keyword.FLYING, Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Patterns.Library.mill(4)
    }

    triggeredAbility {
        trigger = Triggers.YourUpkeep
        effect = ConditionalEffect(
            condition = Conditions.Not(Conditions.Delirium(4)),
            effect = Effects.LoseLife(4, EffectTarget.Controller),
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "41"
        artist = "Daarken"
        imageUri =
            "https://cards.scryfall.io/normal/front/d/d/dd80262b-5f4a-4ea5-88da-f21fca83df3e.jpg?1783937845"
    }
}
