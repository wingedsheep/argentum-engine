package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Painwracker Oni
 * {3}{B}{B}
 * Creature — Demon Spirit
 * 5/4
 * Fear
 * At the beginning of your upkeep, sacrifice a creature if you don't control an Ogre.
 *
 * The trailing "if" is part of the effect, not an intervening-if (no leading "if" clause), so the
 * trigger always goes on the stack and the Ogre check happens on resolution. "An Ogre" is the bare
 * tribal noun, so any permanent with the Ogre subtype counts. "A creature" includes the Oni itself.
 */
val PainwrackerOni = card("Painwracker Oni") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Demon Spirit"
    power = 5
    toughness = 4
    oracleText = "Fear (This creature can't be blocked except by artifact creatures and/or black creatures.)\n" +
        "At the beginning of your upkeep, sacrifice a creature if you don't control an Ogre."

    keywords(Keyword.FEAR)

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.If(
            Conditions.Not(Conditions.ControlPermanentOfType(Subtype("Ogre"))),
            Effects.SacrificeOwn(GameObjectFilter.Creature)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "136"
        artist = "Hideaki Takamura"
        flavorText = "\"Blood flows. Blood calls. Blood devours all and only blood remains.\"\n—Ogre chant"
        imageUri = "https://cards.scryfall.io/normal/front/1/b/1b371b32-7758-4dc9-b4c5-1d1df1f1826a.jpg?1783944309"
    }
}
