package com.wingedsheep.mtg.sets.definitions.c13.cards

import com.wingedsheep.sdk.core.Color
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
 * Ophiomancer
 * {2}{B}
 * Creature — Human Shaman
 * 2/2
 * At the beginning of each upkeep, if you control no Snakes, create a 1/1 black Snake creature
 * token with deathtouch.
 *
 * "Each upkeep" — every player's upkeep, not just yours. "If you control no Snakes" is an
 * intervening-if (CR 603.4): it doesn't trigger while you control a Snake, and it does nothing
 * if you gained one before it resolves. "Snakes" is a bare subtype noun, so it counts any
 * permanent you control with the Snake subtype, not only the tokens this makes.
 */
val Ophiomancer = card("Ophiomancer") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Shaman"
    power = 2
    toughness = 2
    oracleText = "At the beginning of each upkeep, if you control no Snakes, create a 1/1 black Snake " +
        "creature token with deathtouch."

    triggeredAbility {
        trigger = Triggers.anyPlayer.beginningOf(Step.UPKEEP)
        interveningIf = Conditions.YouControl(GameObjectFilter.Permanent.withSubtype(Subtype.SNAKE), negate = true)
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.BLACK),
            creatureTypes = setOf("Snake"),
            keywords = setOf(Keyword.DEATHTOUCH),
            imageUri = "https://cards.scryfall.io/normal/front/8/b/8b9be7c2-efe5-4d37-a43e-ddcd50a77aa9.jpg?1783911114"
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "84"
        artist = "John Stanko"
        flavorText = "\"There are dark, ancient arts that fascinate even me.\"\n—Sorin Markov"
        imageUri = "https://cards.scryfall.io/normal/front/6/6/66d80dd1-b944-4cb2-8578-b4dbcabbbc1e.jpg?1783939675"
        ruling(
            "2013-10-17",
            "Ophiomancer's ability considers any creature you control with the creature type Snake, " +
                "not just the tokens Ophiomancer creates."
        )
        ruling(
            "2013-10-17",
            "If the ability does trigger, but you control a Snake when it tries to resolve, the " +
                "ability will do nothing. No Snake token will be created."
        )
    }
}
