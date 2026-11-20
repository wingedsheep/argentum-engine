package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Master of Waves
 * {3}{U}
 * Creature — Merfolk Wizard
 * 2/1
 * Protection from red
 * Elemental creatures you control get +1/+1.
 * When this creature enters, create a number of 1/0 blue Elemental creature tokens equal to your
 * devotion to blue.
 *
 * The 1/0 tokens live on the lord: they enter as 2/1 while the Master is on the battlefield, and
 * die to state-based actions once it leaves (unless something else props up their toughness).
 * Devotion is counted as the trigger resolves, so the Master's own {U} counts if it's still there.
 * The Master is a Merfolk, not an Elemental, so its own lord doesn't pump it.
 */
val MasterOfWaves = card("Master of Waves") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Merfolk Wizard"
    power = 2
    toughness = 1
    oracleText = "Protection from red\n" +
        "Elemental creatures you control get +1/+1.\n" +
        "When this creature enters, create a number of 1/0 blue Elemental creature tokens equal to " +
        "your devotion to blue. (Each {U} in the mana costs of permanents you control counts toward " +
        "your devotion to blue.)"

    keywordAbility(KeywordAbility.protectionFrom(Color.RED))

    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 1,
            filter = GroupFilter(GameObjectFilter.Creature.withSubtype(Subtype.ELEMENTAL).youControl())
        )
    }

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateToken(
            count = DynamicAmounts.devotionTo(Color.BLUE),
            power = 1,
            toughness = 0,
            colors = setOf(Color.BLUE),
            creatureTypes = setOf("Elemental"),
        )
        description = "When this creature enters, create a number of 1/0 blue Elemental creature tokens " +
            "equal to your devotion to blue."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "53"
        artist = "Karl Kopinski"
        imageUri = "https://cards.scryfall.io/normal/front/1/9/19242db3-660d-4d66-800c-d6cee636bcaf.jpg?1783939795"
        ruling(
            "2013-09-15",
            "Use your devotion to blue as the triggered ability resolves to determine how many tokens are " +
                "put onto the battlefield. If Master of Waves is still on the battlefield at that time, its " +
                "mana cost will count toward your devotion to blue."
        )
        ruling("2013-09-15", "Barring other effects, the Elemental tokens will enter the battlefield as 2/1 creatures.")
        ruling(
            "2013-09-15",
            "If Master of Waves leaves the battlefield, and nothing else is raising the toughness of the " +
                "Elemental tokens above 0, the Elemental tokens will die and subsequently cease to exist."
        )
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
    }
}
