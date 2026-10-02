package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Mite Overseer — Phyrexia: All Will Be One #404 (Jumpstart)
 * {3}{W}
 * Creature — Phyrexian Soldier
 * 4/2
 * First strike
 * During your turn, creature tokens you control get +1/+0 and have first strike.
 * {3}{W/P}: Create a 1/1 colorless Phyrexian Mite artifact creature token with toxic 1 and
 * "This token can't block."
 *
 * The "during your turn" static is two [Conditions.IsYourTurn]-gated statics over
 * creature tokens you control (pump + first strike).
 */
val MiteOverseer = card("Mite Overseer") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Phyrexian Soldier"
    power = 4
    toughness = 2
    oracleText = "First strike\n" +
        "During your turn, creature tokens you control get +1/+0 and have first strike.\n" +
        "{3}{W/P}: Create a 1/1 colorless Phyrexian Mite artifact creature token with toxic 1 and " +
        "\"This token can't block.\" (Players dealt combat damage by it also get a poison counter. " +
        "{W/P} can be paid with either {W} or 2 life.)"

    keywords(Keyword.FIRST_STRIKE)

    staticAbility {
        condition = Conditions.IsYourTurn
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 0,
            filter = GroupFilter(GameObjectFilter.Creature.token().youControl())
        )
    }

    staticAbility {
        condition = Conditions.IsYourTurn
        ability = GrantKeyword(Keyword.FIRST_STRIKE, GroupFilter(GameObjectFilter.Creature.token().youControl()))
    }

    activatedAbility {
        cost = Costs.Mana("{3}{W/P}")
        effect = Effects.CreatePhyrexianMite(1)
        description = "{3}{W/P}: Create a 1/1 colorless Phyrexian Mite artifact creature token with toxic 1 and \"This token can't block.\""
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "404"
        artist = "Néstor Ossandón Leal"
        imageUri = "https://cards.scryfall.io/normal/front/a/5/a55a5edb-edf3-4f6c-90b8-1780b1e73997.jpg?1783917920"
    }
}
