package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.MustAttack

/**
 * Sprinting Warbrute
 * {4}{R}
 * Creature — Ogre Berserker
 * 5/4
 * This creature attacks each combat if able.
 * Dash {3}{R}
 *
 * [MustAttack] with its default source-only filter; `dash` is the builder property that adds the
 * `KeywordAbility.Dash` the cast enumerator reads (as on Pitiless Horde).
 */
val SprintingWarbrute = card("Sprinting Warbrute") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Ogre Berserker"
    power = 5
    toughness = 4
    oracleText = "This creature attacks each combat if able.\n" +
        "Dash {3}{R} (You may cast this spell for its dash cost. If you do, it gains haste, and it's returned from the battlefield to its owner's hand at the beginning of the next end step.)"

    staticAbility {
        ability = MustAttack()
    }

    dash = "{3}{R}"

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "157"
        artist = "Lake Hurwitz"
        imageUri = "https://cards.scryfall.io/normal/front/a/2/a215bde8-1680-4aea-9d0f-2d6d80a592e3.jpg"
    }
}
