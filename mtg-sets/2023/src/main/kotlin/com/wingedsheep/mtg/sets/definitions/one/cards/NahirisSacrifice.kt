package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Nahiri's Sacrifice — Phyrexia: All Will Be One #142
 * {1}{R} · Sorcery
 *
 * As an additional cost to cast this spell, sacrifice an artifact or creature with mana value X.
 * Nahiri's Sacrifice deals X damage divided as you choose among any number of target creatures.
 *
 * X is announced with the spell (the mana cost has no {X}; the sacrifice cost is what pins it), so
 * the cost's filter reads `manaValueEqualsX()` and the damage and target cap read the spell's X.
 * The engine offers one cast per mana value you could sacrifice, each with its X fixed, and the
 * division is announced with the targets (CR 601.2d).
 */
val NahirisSacrifice = card("Nahiri's Sacrifice") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "As an additional cost to cast this spell, sacrifice an artifact or creature with mana value X.\n" +
        "Nahiri's Sacrifice deals X damage divided as you choose among any number of target creatures."

    additionalCost(
        Costs.additional.SacrificePermanent((GameObjectFilter.Artifact or GameObjectFilter.Creature).manaValueEqualsX())
    )

    spell {
        targets(
            TargetFilter(GameObjectFilter.Creature),
            minCount = 0,
            unlimited = true,
            dynamicMaxCount = DynamicAmounts.xValue(),
        )
        effect = Effects.DividedDamage(total = 0, dynamicTotal = DynamicAmounts.xValue())
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "142"
        artist = "Andrey Kuzinskiy"
        imageUri = "https://cards.scryfall.io/normal/front/9/8/98395039-1353-4fa8-b51d-c4e1cadf9263.jpg?1783918025"
        ruling("2023-02-04", "You choose the value of X, the targets, and how damage will be divided as you cast Nahiri's Sacrifice.")
        ruling("2023-02-04", "You can't choose more than X targets, and each chosen target must receive at least 1 damage.")
        ruling("2023-02-04", "If some of the targets of the last ability become illegal, the original division of damage still applies, but the damage that would have been dealt to illegal targets isn't dealt at all.")
    }
}
