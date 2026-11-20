package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Fanatic of Mogis
 * {3}{R}
 * Creature — Minotaur Shaman
 * 4/2
 * When this creature enters, it deals damage to each opponent equal to your devotion to red.
 *
 * Devotion is counted as the trigger resolves, including the Fanatic itself if it's still on the
 * battlefield.
 */
val FanaticOfMogis = card("Fanatic of Mogis") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Minotaur Shaman"
    power = 4
    toughness = 2
    oracleText = "When this creature enters, it deals damage to each opponent equal to your devotion to " +
        "red. (Each {R} in the mana costs of permanents you control counts toward your devotion to red.)"

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.DealDamage(
            DynamicAmounts.devotionTo(Color.RED),
            EffectTarget.PlayerRef(Player.EachOpponent)
        )
        description = "When this creature enters, it deals damage to each opponent equal to your devotion " +
            "to red."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "121"
        artist = "Matt Stewart"
        imageUri = "https://cards.scryfall.io/normal/front/5/8/581eaadb-442b-40c9-b8f9-9e19d5eba824.jpg?1783939763"
        ruling(
            "2013-09-15",
            "Numeric mana symbols ({0}, {1}, and so on) in mana costs of permanents you control don't count " +
                "toward your devotion to any color."
        )
        ruling(
            "2013-09-15",
            "Mana symbols in the text boxes of permanents you control don't count toward your devotion to any color."
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
