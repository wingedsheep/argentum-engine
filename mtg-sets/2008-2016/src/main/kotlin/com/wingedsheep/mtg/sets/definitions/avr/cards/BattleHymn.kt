package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Battle Hymn
 * {1}{R}
 * Instant
 *
 * Add {R} for each creature you control.
 *
 * A spell, not a mana ability: it uses the stack and counts creatures as it resolves (ruling).
 */
val BattleHymn = card("Battle Hymn") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Add {R} for each creature you control."

    spell {
        effect = Effects.AddMana(Color.RED, DynamicAmounts.creaturesYouControl())
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "128"
        artist = "Nils Hamm"
        flavorText = "\"A church is just wood and walls. A congregation is the faithful praising the " +
            "angels.\"\n—Kolman, elder of Gatstaf"
        imageUri = "https://cards.scryfall.io/normal/front/4/3/43b5d46e-7054-44f8-9a14-b412f2f0ab86.jpg?1783940689"
        ruling(
            "2012-05-01",
            "Battle Hymn uses the stack and can be responded to. The amount of mana produced is based on " +
                "the number of creatures you control when Battle Hymn resolves."
        )
    }
}
