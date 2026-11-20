package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Nylea's Disciple
 * {2}{G}{G}
 * Creature — Centaur Archer
 * 3/3
 * When this creature enters, you gain life equal to your devotion to green.
 *
 * Devotion is counted as the trigger resolves (ruling), so the Disciple's own {G}{G} counts if
 * it's still on the battlefield then.
 */
val NyleasDisciple = card("Nylea's Disciple") {
    manaCost = "{2}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Centaur Archer"
    power = 3
    toughness = 3
    oracleText = "When this creature enters, you gain life equal to your devotion to green. " +
        "(Each {G} in the mana costs of permanents you control counts toward your devotion to green.)"

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.GainLife(DynamicAmounts.devotionTo(Color.GREEN))
        description = "When this creature enters, you gain life equal to your devotion to green."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "167"
        artist = "Trevor Claxton"
        imageUri = "https://cards.scryfall.io/normal/front/3/6/365d716a-1435-48ac-b85f-2cceca1056dd.jpg?1783939742"
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
                "color, you count the number of mana symbols of that color among the mana costs of permanents " +
                "you control as the ability resolves. The permanent with that ability will be counted if it's " +
                "still on the battlefield at that time."
        )
    }
}
