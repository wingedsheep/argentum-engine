package com.wingedsheep.mtg.sets.definitions.sos.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.increment
import com.wingedsheep.sdk.model.Rarity

/**
 * Cuboid Colony — Secrets of Strixhaven #183
 * {G}{U} · Creature — Insect · 1/1
 *
 * Flash
 * Flying, trample
 * Increment (Whenever you cast a spell, if the amount of mana you spent is greater than this
 * creature's power or toughness, put a +1/+1 counter on this creature.)
 *
 * A 1/1 with Increment: because its threshold is `min(1, 1)` = 1, any spell costing 2+ mana grows
 * it. As counters accumulate the bar rises with its projected power/toughness.
 */
val CuboidColony = card("Cuboid Colony") {
    manaCost = "{G}{U}"
    colorIdentity = "GU"
    typeLine = "Creature — Insect"
    power = 1
    toughness = 1
    oracleText = "Flash\nFlying, trample\nIncrement (Whenever you cast a spell, if the amount of " +
        "mana you spent is greater than this creature's power or toughness, put a +1/+1 counter " +
        "on this creature.)"

    keywords(Keyword.FLASH, Keyword.FLYING, Keyword.TRAMPLE)
    increment()

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "183"
        artist = "Alexandre Honoré"
        flavorText = "It's chaotic in the midst of the swarm—only when you step back does the " +
            "pattern emerge."
        imageUri = "https://cards.scryfall.io/normal/front/6/3/6384d135-7780-4d75-9e95-71bce506948e.jpg?1775938265"
    }
}
