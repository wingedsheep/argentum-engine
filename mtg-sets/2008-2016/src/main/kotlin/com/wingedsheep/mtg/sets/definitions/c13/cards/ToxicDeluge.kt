package com.wingedsheep.mtg.sets.definitions.c13.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.unaryMinus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Toxic Deluge
 * {2}{B}
 * Sorcery
 * As an additional cost to cast this spell, pay X life.
 * All creatures get -X/-X until end of turn.
 *
 * X is chosen and paid at cast time through [Costs.additional.PayXLife] (capped at the caster's
 * life total); the engine surfaces the paid amount as the spell's X, which the group -X/-X reads.
 */
val ToxicDeluge = card("Toxic Deluge") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "As an additional cost to cast this spell, pay X life.\n" +
        "All creatures get -X/-X until end of turn."

    additionalCost(Costs.additional.PayXLife())

    spell {
        effect = Patterns.Group.modifyStatsForAll(
            power = -DynamicAmounts.xValue(),
            toughness = -DynamicAmounts.xValue(),
            filter = GroupFilter.AllCreatures,
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "96"
        artist = "Svetlin Velinov"
        flavorText = "\"It's a difficult task to quarantine a plague that moves with the clouds.\"\n—Esara, healer adept"
        imageUri = "https://cards.scryfall.io/normal/front/5/6/564caf57-4ba5-4993-a35e-945699c94eb7.jpg?1783939672"
        ruling("2020-08-07", "All creatures on the battlefield when Toxic Deluge resolves are affected. Ones that enter the battlefield or become creatures later in the turn are not.")
        ruling("2020-08-07", "If you cast Toxic Deluge without paying its mana cost, you'll still choose a value for X and pay X life. This is because it doesn't have {X} in its mana cost.")
    }
}
