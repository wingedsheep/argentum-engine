package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Thassa's Rebuff
 * {1}{U}
 * Instant
 * Counter target spell unless its controller pays {X}, where X is your devotion to blue.
 *
 * [Effects.CounterUnlessDynamicPays] over [DynamicAmounts.devotionTo] blue — devotion is counted as
 * the spell resolves, for Thassa's Rebuff's controller. With devotion 0 the tax is {0}, so nothing is
 * countered.
 */
val ThassasRebuff = card("Thassa's Rebuff") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Counter target spell unless its controller pays {X}, where X is your devotion to blue. " +
        "(Each {U} in the mana costs of permanents you control counts toward your devotion to blue.)"

    spell {
        target(TargetFilter.SpellOnStack)
        effect = Effects.CounterUnlessDynamicPays(DynamicAmounts.devotionTo(Color.BLUE))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "54"
        artist = "Lucas Graciano"
        flavorText = "To resist Thassa is to deny the rising tide."
        imageUri = "https://cards.scryfall.io/normal/front/8/1/816a6ff7-cede-4346-b3e6-aee33aefac3a.jpg?1783939563"

        ruling(
            "2013-09-15",
            "Numeric mana symbols ({0}, {1}, and so on) in mana costs of permanents you control don't " +
                "count toward your devotion to any color."
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
