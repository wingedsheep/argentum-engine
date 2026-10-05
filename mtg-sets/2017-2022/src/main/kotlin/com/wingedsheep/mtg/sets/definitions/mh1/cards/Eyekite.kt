package com.wingedsheep.mtg.sets.definitions.mh1.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.ModifyStats

/**
 * Eyekite
 * {1}{U}
 * Creature — Drake
 * 1/2
 * Flying
 * This creature gets +2/+0 as long as you've drawn two or more cards this turn.
 *
 * A self-scoped [ModifyStats] wrapped in a [ConditionalStaticAbility] gated on
 * [Conditions.YouDrewCardsThisTurn] (threshold 2). The tracker counts only genuine draws for the
 * whole turn, so draws made before Eyekite entered count too (both printed rulings).
 */
val Eyekite = card("Eyekite") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Drake"
    power = 1
    toughness = 2
    oracleText = "Flying\nThis creature gets +2/+0 as long as you've drawn two or more cards this turn."

    keywords(Keyword.FLYING)

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = ModifyStats(2, 0, Filters.Self),
            condition = Conditions.YouDrewCardsThisTurn(2),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "49"
        artist = "Dan Murayama Scott"
        flavorText = "\"This one will need direction. After all, each of its eyes is larger than its brain.\"\n" +
            "—Cyla, Lord of the Aerie"
        imageUri = "https://cards.scryfall.io/normal/front/4/9/49f7bda8-949a-42aa-9254-f39b791cb9a8.jpg?1783933145"
        ruling("2019-06-14", "Eyekite's effect applies even if you drew two or more cards only before Eyekite entered the battlefield.")
        ruling("2019-06-14", "If a spell or ability causes you to put cards into your hand without specifically using the word \"draw,\" Eyekite's effect doesn't count them.")
    }
}
