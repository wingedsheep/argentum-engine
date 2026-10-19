package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Go-Shintai of Lost Wisdom — Kamigawa: Neon Dynasty #55 (canonical printing)
 * {1}{U} · Legendary Enchantment Creature — Shrine · 0/4
 *
 * Flying
 * At the beginning of your end step, you may pay {1}. When you do, target player mills X cards,
 * where X is the number of Shrines you control.
 *
 * "When you do" is a reflexive trigger (CR 603.12): the {1} is paid as the end-step trigger
 * resolves, and only then does the mill ability go on the stack and choose its target. X is read
 * when that reflexive ability resolves and includes the Go-Shintai itself.
 */
val GoShintaiOfLostWisdom = card("Go-Shintai of Lost Wisdom") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Enchantment Creature — Shrine"
    power = 0
    toughness = 4
    oracleText = "Flying\nAt the beginning of your end step, you may pay {1}. When you do, target " +
        "player mills X cards, where X is the number of Shrines you control. (To mill a card, a " +
        "player puts the top card of their library into their graveyard.)"

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        effect = Effects.ReflexiveTrigger(
            action = Effects.PayMana("{1}"),
            descriptionOverride = "You may pay {1}. When you do, target player mills X cards, where X " +
                "is the number of Shrines you control."
        ) {
            val victim = target(Targets.Player)
            effect = Patterns.Library.mill(
                DynamicAmounts.battlefield(Player.You, GameObjectFilter.Any.withSubtype("Shrine")).count(),
                victim,
            )
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "55"
        artist = "Johannes Voss"
        flavorText = "It seeks those driven by curiosity and offers enlightenment."
        imageUri = "https://cards.scryfall.io/normal/front/7/e/7ec1192c-25b8-4975-ae6c-86be28ac1840.jpg?1783923903"
    }
}
