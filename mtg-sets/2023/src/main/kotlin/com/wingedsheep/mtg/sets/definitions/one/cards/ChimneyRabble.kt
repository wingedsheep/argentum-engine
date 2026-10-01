package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Chimney Rabble
 * {3}{R}
 * Creature — Phyrexian Goblin Warrior
 * 3/3
 *
 * Haste
 * When this creature enters, create a 1/1 red Phyrexian Goblin creature token.
 */
val ChimneyRabble = card("Chimney Rabble") {
    manaCost = "{3}{R}"
    typeLine = "Creature — Phyrexian Goblin Warrior"
    power = 3
    toughness = 3
    oracleText = "Haste\n" +
        "When this creature enters, create a 1/1 red Phyrexian Goblin creature token."

    keywords(Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.RED),
            creatureTypes = setOf("Phyrexian", "Goblin"),
            imageUri = "https://cards.scryfall.io/normal/front/3/6/3663e79b-2bf9-44af-a638-c0ad9067d8d4.jpg?1783918169",
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "126"
        artist = "Alexander Mokhov"
        flavorText = "\"They're violent, headstrong, and wildly unpredictable. It should be amusing to see " +
            "how Norn's vaunted tactics try to cope with them.\"\n—Urabrask"
        imageUri = "https://cards.scryfall.io/normal/front/5/6/5668699d-8df8-426b-a04a-99cfe55e570b.jpg?1783918032"
    }
}
