package com.wingedsheep.mtg.sets.definitions.m13.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Touch of the Eternal
 * {5}{W}{W}
 * Enchantment
 *
 * At the beginning of your upkeep, count the number of permanents you control. Your life total
 * becomes that number.
 *
 * The count is taken on resolution (Touch of the Eternal itself included, if still on the
 * battlefield); setting the life total gains or loses the difference, per the ruling.
 */
val TouchOfTheEternal = card("Touch of the Eternal") {
    manaCost = "{5}{W}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "At the beginning of your upkeep, count the number of permanents you control. " +
        "Your life total becomes that number."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.SetLifeTotal(
            DynamicAmounts.battlefield(Player.You, GameObjectFilter.Permanent).count()
        )
        description = "count the number of permanents you control. Your life total becomes that number."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "37"
        artist = "Christopher Moeller"
        flavorText = "\"Time is a vigilant tyrant, but there are those who have broken free of its gaze.\"\n" +
            "—Laikas, Erunian monk"
        imageUri = "https://cards.scryfall.io/normal/front/5/5/55c5f0c2-99e6-42b7-aa16-61d5815d060d.jpg?1783940511"
        ruling("2012-07-01", "This change of life total counts as life gain or life loss, as appropriate. Other effects that interact with life gain or life loss will interact with this effect accordingly.")
    }
}
