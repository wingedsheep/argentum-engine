package com.wingedsheep.mtg.sets.definitions.mat.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.minus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Arni Metalbrow — March of the Machine: The Aftermath #16
 * {2}{R} · Legendary Creature — Human Berserker · 3/3
 *
 * Whenever a creature you control attacks or enters attacking, you may pay {1}{R}. If you do, you
 * may put a creature card with mana value less than that creature's mana value from your hand onto
 * the battlefield tapped and attacking.
 *
 * One disjunctive trigger: a declared attacker (`attacks()`) or a creature put onto the battlefield
 * attacking (`entersAttacking()`). The creature Arni puts in enters attacking, so it re-triggers
 * Arni — the chain the card is built around. "That creature's mana value" is the triggering
 * creature's, read on resolution.
 */
val ArniMetalbrow = card("Arni Metalbrow") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Human Berserker"
    power = 3
    toughness = 3
    oracleText = "Whenever a creature you control attacks or enters attacking, you may pay {1}{R}. " +
        "If you do, you may put a creature card with mana value less than that creature's mana value " +
        "from your hand onto the battlefield tapped and attacking."

    triggeredAbility {
        trigger = Triggers.or(
            Triggers.a(GameObjectFilter.Creature.youControl()).attacks(),
            Triggers.a(GameObjectFilter.Creature.youControl()).entersAttacking()
        )
        effect = Effects.MayPay(
            cost = ManaCost.parse("{1}{R}"),
            then = Patterns.Hand.putFromHand(
                filter = GameObjectFilter.Creature.manaValueAtMostDynamic(
                    DynamicAmounts.manaValueOf(EffectTarget.TriggeringEntity) - 1
                ),
                entersAttacking = true,
                prompt = "You may put a creature card with lesser mana value onto the battlefield tapped and attacking"
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "16"
        artist = "Tyler Jacobson"
        imageUri = "https://cards.scryfall.io/normal/front/a/e/aeb3069f-5f5a-4e11-8a63-46399eb85a95.jpg?1783916524"
    }
}
