package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.CardNumericProperty

/**
 * Might of the Old Ways (Innistrad: Midnight Hunt #189)
 * {1}{G}
 * Instant
 * Target creature gets +2/+2 until end of turn.
 * Coven — Then if you control three or more creatures with different powers, draw a card.
 *
 * The coven check runs after the pump resolves, so it sees the targeted creature's boosted power.
 */
val MightOfTheOldWays = card("Might of the Old Ways") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Target creature gets +2/+2 until end of turn.\n" +
        "Coven — Then if you control three or more creatures with different powers, draw a card."

    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(2, 2, t) then Effects.If(
            condition = Conditions.CompareAmounts(
                left = DynamicAmounts.battlefield(
                    Player.You,
                    GameObjectFilter.Creature,
                ).distinctValues(CardNumericProperty.POWER),
                operator = ComparisonOperator.GTE,
                right = 3,
            ),
            then = Effects.DrawCards(1),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "189"
        artist = "Zezhou Chen"
        flavorText = "Brimming with power from their disrupted ritual, the Dawnhart witches lashed out at the werewolf intruders."
        imageUri = "https://cards.scryfall.io/normal/front/6/a/6a853679-e9dd-4c05-8688-248de8f12760.jpg?1783925577"
        ruling("2021-09-24", "The effects of Might of the Old Ways happen in the listed order, so the +2/+2 bonus happens before you check the powers of creatures you control. For example, if you control two 2/2 creatures and a 5/5 creatures, choosing one of the 2/2 creatures as the target will allow you to draw a card. Conversely, if you control a 1/1 creature, a 3/3 creature, and a 5/5 creature, targeting either of the two smaller creatures will not allow you to draw a card.")
    }
}
