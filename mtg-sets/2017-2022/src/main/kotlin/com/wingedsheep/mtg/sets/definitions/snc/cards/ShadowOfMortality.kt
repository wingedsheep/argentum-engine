package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.minus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Shadow of Mortality
 * {13}{B}{B}
 * Creature — Avatar
 * 7/7
 * If your life total is less than your starting life total, this spell costs {X} less to cast,
 * where X is the difference.
 *
 * A self-cast dynamic reduction of the generic part only: starting life minus current life. The
 * engine clamps a dynamic reduction at 0, so at or above your starting life there is no discount
 * (and never a tax).
 */
val ShadowOfMortality = card("Shadow of Mortality") {
    manaCost = "{13}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Avatar"
    power = 7
    toughness = 7
    oracleText = "If your life total is less than your starting life total, this spell costs {X} less to cast, where X is the difference."

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGenericBy(
                CostReductionSource.Dynamic(
                    DynamicAmounts.startingLifeTotal(Player.You) - DynamicAmounts.yourLifeTotal()
                ),
            ),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "94"
        artist = "Robin Olausson"
        flavorText = "\"Build your towers as tall as you want. The shadow always reaches higher.\"\n—Message scrawled on an alley wall"
        imageUri = "https://cards.scryfall.io/normal/front/4/0/40cbb0db-94e9-4988-b900-ced4f9e6d4ed.jpg?1783923125"
        ruling("2022-04-29", "In a Two-Headed Giant game, your starting life total is the life total your team started at, and your current life total is your team's current life total.")
    }
}
