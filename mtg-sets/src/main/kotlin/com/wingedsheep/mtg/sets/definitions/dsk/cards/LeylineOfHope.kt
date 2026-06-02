package com.wingedsheep.mtg.sets.definitions.dsk.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.ModifyLifeGain
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.conditions.Compare
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Leyline of Hope (DSK #18)
 * {2}{W}{W}  Enchantment
 *
 * If this card is in your opening hand, you may begin the game with it on the battlefield.
 * If you would gain life, you gain that much life plus 1 instead.
 * As long as you have at least 7 life more than your starting life total, creatures you
 * control get +2/+2.
 */
val LeylineOfHope = card("Leyline of Hope") {
    manaCost = "{2}{W}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "If this card is in your opening hand, you may begin the game with it on the battlefield.\n" +
        "If you would gain life, you gain that much life plus 1 instead.\n" +
        "As long as you have at least 7 life more than your starting life total, creatures you control get +2/+2."

    leyline()

    replacementEffect(
        ModifyLifeGain(
            multiplier = 1,
            modifier = 1,
            appliesTo = EventPattern.LifeGainEvent(player = Player.You)
        )
    )

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = ModifyStats(
                powerBonus = 2,
                toughnessBonus = 2,
                filter = GroupFilter.AllCreaturesYouControl
            ),
            condition = Compare(
                left = DynamicAmount.LifeTotal(Player.You),
                operator = ComparisonOperator.GTE,
                right = DynamicAmount.Add(
                    DynamicAmount.StartingLifeTotal(Player.You),
                    DynamicAmount.Fixed(7)
                )
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "18"
        artist = "Sergey Glushakov"
        imageUri = "https://cards.scryfall.io/normal/front/4/0/40960e47-3065-485e-aede-29a62411034e.jpg?1726285926"
    }
}
