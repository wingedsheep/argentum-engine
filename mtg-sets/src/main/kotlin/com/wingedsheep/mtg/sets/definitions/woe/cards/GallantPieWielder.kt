package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GrantKeyword

/**
 * Gallant Pie-Wielder
 * {2}{W}
 * Creature — Dwarf Knight
 * 2/3
 *
 * First strike
 * Celebration — This creature has double strike as long as two or more nonland permanents entered
 * the battlefield under your control this turn.
 *
 * The static half of the Celebration ability word (CR 207.2c — italic flavor, no rules meaning),
 * same shape as [ArmoryMice] but granting a keyword instead of a stat bonus: a
 * [ConditionalStaticAbility] wrapping a [GrantKeyword] over [Filters.Self], re-evaluated every
 * projection so double strike appears the instant the second nonland permanent enters — including
 * between the first-strike and regular combat damage steps.
 *
 * Printed first strike stays a plain keyword; CR 702.4b makes a creature with both first strike and
 * double strike deal damage in the first-strike step and again in the regular step, so the two
 * coexist without special handling.
 */
val GallantPieWielder = card("Gallant Pie-Wielder") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Dwarf Knight"
    power = 2
    toughness = 3
    oracleText = "First strike\n" +
        "Celebration — This creature has double strike as long as two or more nonland permanents " +
        "entered the battlefield under your control this turn."

    keywords(Keyword.FIRST_STRIKE)

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.DOUBLE_STRIKE, Filters.Self),
            condition = Conditions.Celebration,
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "15"
        artist = "Matt Forsyth"
        flavorText = "\"Time for your just desserts, redcap scum!\""
        imageUri = "https://cards.scryfall.io/normal/front/e/0/e053d330-d0a2-4468-afba-42bf165b8fbf.jpg?1783915132"
    }
}
