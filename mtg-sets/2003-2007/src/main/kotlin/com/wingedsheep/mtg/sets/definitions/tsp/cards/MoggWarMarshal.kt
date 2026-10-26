package com.wingedsheep.mtg.sets.definitions.tsp.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.echo
import com.wingedsheep.sdk.model.Rarity

/**
 * Mogg War Marshal
 * {1}{R}
 * Creature — Goblin Warrior
 * 1/1
 * Echo {1}{R}
 * When this creature enters or dies, create a 1/1 red Goblin creature token.
 */
val MoggWarMarshal = card("Mogg War Marshal") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Goblin Warrior"
    power = 1
    toughness = 1
    oracleText = "Echo {1}{R} (At the beginning of your upkeep, if this came under your control since the " +
        "beginning of your last upkeep, sacrifice it unless you pay its echo cost.)\n" +
        "When this creature enters or dies, create a 1/1 red Goblin creature token."

    echo(ManaCost.parse("{1}{R}"))

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.RED),
            creatureTypes = setOf("Goblin"),
        )
    }

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.RED),
            creatureTypes = setOf("Goblin"),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "170"
        artist = "Wayne England"
        imageUri = "https://cards.scryfall.io/normal/front/8/b/8b9e0bdb-b615-447a-b80d-d7244c25c56e.jpg"
    }
}
