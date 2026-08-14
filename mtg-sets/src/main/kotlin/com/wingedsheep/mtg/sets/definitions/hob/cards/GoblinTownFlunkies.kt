package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Goblin-town Flunkies
 * {1}{R}
 * Creature — Goblin Soldier
 * 1/1
 *
 * Haste
 * When this creature enters, amass Goblins 1.
 */
val GoblinTownFlunkies = card("Goblin-town Flunkies") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Goblin Soldier"
    oracleText = "Haste\n" +
        "When this creature enters, amass Goblins 1. (Put a +1/+1 counter on an Army you control. " +
        "It's also a Goblin. If you don't control an Army, create a 0/0 black Goblin Army creature " +
        "token first.)"
    power = 1
    toughness = 1

    keywords(Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.Amass(1, "Goblin")
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "100"
        artist = "Jason Kang"
        flavorText = "Goblins made no beautiful things, but they made many clever ones."
        imageUri = "https://cards.scryfall.io/normal/front/c/c/ccff7382-8609-494c-aeee-cd1436456dd0.jpg?1785497117"
    }
}
