package com.wingedsheep.mtg.sets.definitions.m13.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Sublime Archangel
 * {2}{W}{W}
 * Creature — Angel
 * 4/3
 *
 * Flying
 * Exalted
 * Other creatures you control have exalted. (If a creature has multiple instances of exalted,
 * each triggers separately.)
 *
 * The grant is a plain [GrantKeyword] of [Keyword.EXALTED]; projection tallies each grant as its own
 * instance, so a lone attacker gets +1/+1 for every exalted instance among permanents you control.
 */
val SublimeArchangel = card("Sublime Archangel") {
    manaCost = "{2}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Angel"
    oracleText = "Flying\n" +
        "Exalted (Whenever a creature you control attacks alone, that creature gets +1/+1 until end of turn.)\n" +
        "Other creatures you control have exalted. (If a creature has multiple instances of exalted, " +
        "each triggers separately.)"
    power = 4
    toughness = 3

    keywords(Keyword.FLYING, Keyword.EXALTED)

    staticAbility {
        ability = GrantKeyword(Keyword.EXALTED, GroupFilter.OtherCreaturesYouControl)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "36"
        artist = "Cynthia Sheppard"
        imageUri = "https://cards.scryfall.io/normal/front/f/5/f5cc38bc-55a4-446a-b054-48fb90216946.jpg?1783940511"
        ruling("2012-07-01", "Whenever a creature you control attacks alone, count the number of instances of exalted among permanents you control. After those abilities resolve, that’s how many times the creature will get +1/+1.")
    }
}
