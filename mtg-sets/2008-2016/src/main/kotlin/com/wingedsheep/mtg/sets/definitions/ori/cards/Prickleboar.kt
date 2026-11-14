package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Prickleboar
 * {4}{R}
 * Creature — Boar
 * 3/3
 * During your turn, this creature gets +2/+0 and has first strike.
 *
 * Two [ConditionalStaticAbility]s gated by [Conditions.IsYourTurn] (Sunlit Hoplite's
 * your-turn first strike plus a self [ModifyStats]), both recomputed at projection.
 */
val Prickleboar = card("Prickleboar") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Boar"
    power = 3
    toughness = 3
    oracleText = "During your turn, this creature gets +2/+0 and has first strike. " +
        "(It deals combat damage before creatures without first strike.)"

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = ModifyStats(2, 0, GroupFilter.source()),
            condition = Conditions.IsYourTurn,
        )
    }

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.FIRST_STRIKE, GroupFilter.source()),
            condition = Conditions.IsYourTurn,
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "158"
        artist = "Jesper Ejsing"
        flavorText = "\"Canyons in the Phoberos badlands are essentially gates to the Underworld.\"\n—Mirinthes, Akroan captain"
        imageUri = "https://cards.scryfall.io/normal/front/3/1/3191a8fd-2c54-4e47-9d5a-692bb38c811f.jpg?1783938326"
    }
}
