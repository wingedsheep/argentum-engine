package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.CardNumericProperty

/**
 * Go-Shintai of Hidden Cruelty — Kamigawa: Neon Dynasty #97 (canonical printing)
 * {3}{B} · Legendary Enchantment Creature — Shrine · 2/2
 *
 * Deathtouch
 * At the beginning of your end step, you may pay {1}. When you do, destroy target creature with
 * toughness X or less, where X is the number of Shrines you control.
 *
 * "When you do" is a reflexive trigger (CR 603.12): the {1} is paid as the end-step trigger
 * resolves, and only then does the destroy ability go on the stack and choose its target. The
 * toughness cap is a late-bound comparison against the Shrine count (Go-Shintai itself included),
 * checked when the target is chosen and again on resolution.
 */
val GoShintaiOfHiddenCruelty = card("Go-Shintai of Hidden Cruelty") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Enchantment Creature — Shrine"
    power = 2
    toughness = 2
    oracleText = "Deathtouch\nAt the beginning of your end step, you may pay {1}. When you do, " +
        "destroy target creature with toughness X or less, where X is the number of Shrines you control."

    keywords(Keyword.DEATHTOUCH)

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        effect = Effects.ReflexiveTrigger(
            action = Effects.PayMana("{1}"),
            descriptionOverride = "You may pay {1}. When you do, destroy target creature with toughness " +
                "X or less, where X is the number of Shrines you control."
        ) {
            val victim = target(
                TargetFilter.Creature.compareNumericProperty(
                    CardNumericProperty.TOUGHNESS,
                    ComparisonOperator.LTE,
                    DynamicAmounts.battlefield(Player.You, GameObjectFilter.Any.withSubtype("Shrine")).count(),
                )
            )
            effect = Effects.Destroy(victim)
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "97"
        artist = "Johannes Voss"
        flavorText = "It seeks those with hate in their hearts and offers vengeance."
        imageUri = "https://cards.scryfall.io/normal/front/4/5/45e1ff1e-916b-4455-84cb-4125a79d76de.jpg?1783923886"
    }
}
