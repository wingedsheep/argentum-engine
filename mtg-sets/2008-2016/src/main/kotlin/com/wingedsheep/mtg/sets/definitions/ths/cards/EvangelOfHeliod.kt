package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Evangel of Heliod
 * {4}{W}{W}
 * Creature — Human Cleric
 * 1/3
 * When this creature enters, create a number of 1/1 white Soldier creature tokens equal to your
 * devotion to white.
 *
 * Devotion is counted as the trigger resolves, so the Evangel's own {W}{W} counts if it's still on
 * the battlefield.
 */
val EvangelOfHeliod = card("Evangel of Heliod") {
    manaCost = "{4}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Cleric"
    power = 1
    toughness = 3
    oracleText = "When this creature enters, create a number of 1/1 white Soldier creature tokens equal " +
        "to your devotion to white. (Each {W} in the mana costs of permanents you control counts toward " +
        "your devotion to white.)"

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateToken(
            count = DynamicAmounts.devotionTo(Color.WHITE),
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Soldier"),
        )
        description = "When this creature enters, create a number of 1/1 white Soldier creature tokens " +
            "equal to your devotion to white."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "11"
        artist = "Nils Hamm"
        imageUri = "https://cards.scryfall.io/normal/front/b/b/bb914a85-3755-4663-8309-6f6d0319262e.jpg?1783939817"
        ruling(
            "2013-09-15",
            "Numeric mana symbols ({0}, {1}, and so on) in mana costs of permanents you control don't " +
                "count toward your devotion to any color."
        )
        ruling(
            "2013-09-15",
            "Mana symbols in the text boxes of permanents you control don't count toward your devotion to " +
                "any color."
        )
        ruling(
            "2013-09-15",
            "Hybrid mana symbols, monocolored hybrid mana symbols, and Phyrexian mana symbols do count " +
                "toward your devotion to their color(s)."
        )
        ruling(
            "2013-09-15",
            "If an activated ability or triggered ability has an effect that depends on your devotion to a " +
                "color, you count the number of mana symbols of that color among the mana costs of " +
                "permanents you control as the ability resolves. The permanent with that ability will be " +
                "counted if it's still on the battlefield at that time."
        )
    }
}
