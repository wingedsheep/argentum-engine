package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Reverent Hunter
 * {2}{G}
 * Creature — Human Archer
 * 1/1
 * When this creature enters, put a number of +1/+1 counters on it equal to your devotion to green.
 *
 * Devotion is counted as the trigger resolves (ruling), so the Hunter's own {G} counts.
 */
val ReverentHunter = card("Reverent Hunter") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Archer"
    power = 1
    toughness = 1
    oracleText = "When this creature enters, put a number of +1/+1 counters on it equal to your devotion to green. " +
        "(Each {G} in the mana costs of permanents you control counts toward your devotion to green.)"

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.AddDynamicCounters(
            CounterType.PLUS_ONE_PLUS_ONE,
            DynamicAmounts.devotionTo(Color.GREEN),
            EffectTarget.Self
        )
        description = "When this creature enters, put a number of +1/+1 counters on it equal to your devotion to green."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "173"
        artist = "Wesley Burt"
        imageUri = "https://cards.scryfall.io/normal/front/9/7/97b4e736-d19a-42ac-8bcb-b8e6b7b7b539.jpg?1783939738"
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
