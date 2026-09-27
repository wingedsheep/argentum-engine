package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Gutwrencher Oni
 * {3}{B}{B}
 * Creature — Demon Spirit
 * 5/4
 * Trample
 * At the beginning of your upkeep, discard a card if you don't control an Ogre.
 *
 * The trailing "if" is part of the effect, not an intervening-if (no leading "if" clause), so the
 * trigger always goes on the stack and the Ogre check happens on resolution. "An Ogre" is the bare
 * tribal noun, so any permanent with the Ogre subtype counts.
 */
val GutwrencherOni = card("Gutwrencher Oni") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Demon Spirit"
    power = 5
    toughness = 4
    oracleText = "Trample\nAt the beginning of your upkeep, discard a card if you don't control an Ogre."

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.If(
            Conditions.Not(Conditions.ControlPermanentOfType(Subtype("Ogre"))),
            Effects.Discard(1)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "113"
        artist = "Hideaki Takamura"
        flavorText = "\"Blood drips. Blood sings. Blood devours all and only blood remains.\"\n—Ogre chant"
        imageUri = "https://cards.scryfall.io/normal/front/7/6/7698a5da-5c70-4818-927b-3a923314e537.jpg?1783944316"
    }
}
