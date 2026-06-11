package com.wingedsheep.mtg.sets.definitions.tmt.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Raphael, the Nightwatcher
 * {2}{R}{R}
 * Legendary Creature — Mutant Ninja Turtle
 * 2/3
 *
 * Sneak {1}{R}{R} (You may cast this spell for {1}{R}{R} if you also return an
 * unblocked attacker you control to hand during the declare blockers step. He
 * enters tapped and attacking.)
 * Attacking creatures you control have double strike.
 */
val RaphaelTheNightwatcher = card("Raphael, the Nightwatcher") {
    manaCost = "{2}{R}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Mutant Ninja Turtle"
    oracleText = "Sneak {1}{R}{R} (You may cast this spell for {1}{R}{R} if you also return an unblocked attacker you control to hand during the declare blockers step. He enters tapped and attacking.)\nAttacking creatures you control have double strike."
    power = 2
    toughness = 3

    sneak("{1}{R}{R}")

    staticAbility {
        ability = GrantKeyword(
            keyword = Keyword.DOUBLE_STRIKE,
            filter = GroupFilter.AllCreaturesYouControl.attacking()
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "103"
        artist = "Greg Staples"
        flavorText = "\"Time to punch in.\""
        imageUri = "https://cards.scryfall.io/normal/front/6/a/6af50b50-3776-4383-b493-7c5dd732c965.jpg?1769006162"
    }
}
