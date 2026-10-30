package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Reverent Hoplite
 * {4}{W}
 * Creature — Human Soldier
 * 1/2
 *
 * When this creature enters, create a number of 1/1 white Human Soldier creature tokens equal to
 * your devotion to white.
 *
 * A dynamic-count [Effects.CreateToken] over [DynamicAmounts.devotionTo], read as the trigger
 * resolves — the Hoplite's own {W} counts if it's still on the battlefield then (ruling).
 */
val ReverentHoplite = card("Reverent Hoplite") {
    manaCost = "{4}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Soldier"
    power = 1
    toughness = 2
    oracleText = "When this creature enters, create a number of 1/1 white Human Soldier creature tokens equal " +
        "to your devotion to white. (Each {W} in the mana costs of permanents you control counts toward your " +
        "devotion to white.)"

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateToken(
            count = DynamicAmounts.devotionTo(Color.WHITE),
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Human", "Soldier"),
            imageUri = "https://cards.scryfall.io/normal/front/1/e/1e76f0e3-9411-401d-ab38-9c3c64769483.jpg?1783931455",
        )
        description = "When this creature enters, create a number of 1/1 white Human Soldier creature tokens " +
            "equal to your devotion to white."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "33"
        artist = "Sidharth Chaturvedi"
        flavorText = "\"Zeal is the sharpest blade, and piety the strongest shield.\""
        imageUri = "https://cards.scryfall.io/normal/front/5/4/54153b9c-483e-4e5c-a1ab-b1c8a7a657d4.jpg?1783931590"
        ruling(
            "2020-01-24",
            "If an activated ability or triggered ability has an effect that depends on your devotion to a color, " +
                "you count the number of mana symbols of that color among the mana costs of permanents you control " +
                "as the ability resolves. The permanent with that ability will be counted if it's still on the " +
                "battlefield at that time."
        )
    }
}
