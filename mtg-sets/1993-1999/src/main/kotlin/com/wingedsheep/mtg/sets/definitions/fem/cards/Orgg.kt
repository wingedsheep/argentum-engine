package com.wingedsheep.mtg.sets.definitions.fem.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CanOnlyBlockCreaturesWith
import com.wingedsheep.sdk.scripting.CantAttackUnless
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.Exists
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Orgg
 * {3}{R}{R}
 * Creature — Orgg
 * 6/6
 * Trample
 * This creature can't attack if defending player controls an untapped creature with power 3 or greater.
 * This creature can't block creatures with power 3 or greater.
 *
 * The attack clause is the negation of an existence check bound to the *defending* player, which
 * is exactly the player `CantAttackUnless` evaluates its condition against at declaration time.
 * The block clause is written as its complement, the way [BrassclawOrcs] is.
 */
val Orgg = card("Orgg") {
    manaCost = "{3}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Orgg"
    oracleText = "Trample\n" +
        "This creature can't attack if defending player controls an untapped creature with power 3 or greater.\n" +
        "This creature can't block creatures with power 3 or greater."
    power = 6
    toughness = 6

    keywords(Keyword.TRAMPLE)

    staticAbility {
        ability = CantAttackUnless(
            Exists(
                Player.DefendingPlayer,
                Zone.BATTLEFIELD,
                GameObjectFilter.Creature.untapped().powerAtLeast(3),
                negate = true
            )
        )
    }

    staticAbility {
        ability = CanOnlyBlockCreaturesWith(GameObjectFilter.Creature.powerAtMost(2))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "63"
        artist = "Daniel Gelon"
        flavorText = "It's bigger than it thinks."
        imageUri = "https://cards.scryfall.io/normal/front/5/a/5af19ab0-4bd0-4d5f-8d2e-507e4fe87c18.jpg?1783947890"
    }
}
