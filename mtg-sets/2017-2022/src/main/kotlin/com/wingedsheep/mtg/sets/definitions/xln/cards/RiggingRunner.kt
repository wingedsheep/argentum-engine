package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters

/**
 * Rigging Runner
 * {R}
 * Creature — Goblin Pirate
 * 1/1
 *
 * First strike
 * Raid — This creature enters with a +1/+1 counter on it if you attacked this turn.
 */
val RiggingRunner = card("Rigging Runner") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Creature — Goblin Pirate"
    power = 1
    toughness = 1
    oracleText = "First strike\nRaid — This creature enters with a +1/+1 counter on it if you attacked this turn."

    keywords(Keyword.FIRST_STRIKE)

    replacementEffect(EntersWithCounters(
        counterType = CounterType.PLUS_ONE_PLUS_ONE,
        count = 1,
        selfOnly = true,
        condition = Conditions.YouAttackedThisTurn
    ))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "157"
        artist = "Simon Dominic"
        flavorText = "The hook makes him feel brave, and the hat makes him feel fancy."
        imageUri = "https://cards.scryfall.io/normal/front/e/b/eb9983ce-8ca6-450a-9cac-5396ba8e1690.jpg?1783935740"
    }
}
