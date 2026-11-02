package com.wingedsheep.mtg.sets.definitions.m19.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

private val demon = GameObjectFilter.Permanent.withSubtype(Subtype.DEMON)

/**
 * Liliana's Contract
 * {3}{B}{B}
 * Enchantment
 * When this enchantment enters, you draw four cards and you lose 4 life.
 * At the beginning of your upkeep, if you control four or more Demons with different names,
 * you win the game.
 *
 * "Demons with different names" counts each name once (`distinctNames()`, as Maze's End does
 * for Gates); the count is an intervening "if" (CR 603.4), checked as the upkeep begins and
 * again on resolution, per the 2018-07-13 rulings.
 */
val LilianasContract = card("Liliana's Contract") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment"
    oracleText = "When this enchantment enters, you draw four cards and you lose 4 life.\n" +
        "At the beginning of your upkeep, if you control four or more Demons with different " +
        "names, you win the game."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.DrawCards(4) then Effects.LoseLife(4, EffectTarget.Controller)
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        interveningIf = Conditions.CompareAmounts(
            DynamicAmounts.battlefield(Player.You, demon).distinctNames(),
            ComparisonOperator.GTE,
            4
        )
        effect = Effects.WinGame()
        description = "At the beginning of your upkeep, if you control four or more Demons with " +
            "different names, you win the game."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "107"
        artist = "Bastien L. Deharme"
        flavorText = "Immortality is worth any price."
        imageUri = "https://cards.scryfall.io/normal/front/7/5/750e6246-c28d-46ed-9966-26706f6d3172.jpg?1783934567"
        ruling("2018-07-13", "If you don't control four Demons with different names as your upkeep begins, the second ability of Liliana's Contract won't trigger. You can't take any actions during your turn before your upkeep begins.")
        ruling("2018-07-13", "If the second ability does trigger, but you no longer control four Demons with different names as the ability resolves, you won't win the game.")
    }
}
