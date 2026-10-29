package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Sanctum of Calm Waters — Core Set 2021 #68
 * {3}{U} · Legendary Enchantment — Shrine
 *
 * At the beginning of your first main phase, you may draw X cards, where X is the number of
 * Shrines you control. If you do, discard a card.
 *
 * An optional trigger: accepting draws X and then discards one, all during resolution. X is
 * counted on resolution, so with no Shrines left you may still accept, draw zero and discard
 * one (ruling) — declining skips both.
 */
val SanctumOfCalmWaters = card("Sanctum of Calm Waters") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Enchantment — Shrine"
    oracleText = "At the beginning of your first main phase, you may draw X cards, where X is the number " +
        "of Shrines you control. If you do, discard a card."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.PRECOMBAT_MAIN)
        optional = true
        effect = Effects.DrawCards(
            DynamicAmounts.battlefield(Player.You, GameObjectFilter.Any.withSubtype("Shrine")).count()
        ) then Effects.Discard(1)
        description = "At the beginning of your first main phase, you may draw X cards, where X is the number " +
            "of Shrines you control. If you do, discard a card."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "68"
        artist = "Johannes Voss"
        flavorText = "Truth lurks in the depths,\nInvisible from the shore.\nKnowledge inundates."
        imageUri = "https://cards.scryfall.io/normal/front/8/0/80b2fb65-5868-4cab-b45d-e103558ce7dc.jpg?1783930720"
        ruling(
            "2020-06-23",
            "You draw X cards and discard a card all while the triggered ability is resolving. Nothing can happen between the two, and no player may choose to take actions."
        )
        ruling(
            "2020-06-23",
            "If Sanctum of Calm Waters leaves the battlefield while its ability is on the stack, you may control no Shrines as the ability resolves. If so, you may choose to draw zero cards and discard one, or you may choose not to draw zero cards and not discard a card."
        )
        ruling(
            "2020-06-23",
            "Each Shrine has an ability that counts the number of Shrines you control. These abilities include the Shrine they're printed on."
        )
        ruling(
            "2020-06-23",
            "Shrines count only enchantments with the subtype Shrine. Other cards with \"shrine\" in their name (such as Jungle Shrine, Luxa River Shrine, and Nantuko Shrine) don't count."
        )
    }
}
