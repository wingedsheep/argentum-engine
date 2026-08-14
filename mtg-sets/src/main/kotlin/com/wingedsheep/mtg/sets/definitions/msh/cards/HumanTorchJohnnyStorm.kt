package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Human Torch, Johnny Storm — Marvel Super Heroes #136 (uncommon)
 * {2}{R} · Legendary Creature — Human Hero · 2/2
 *
 * Flying
 * Whenever you draw a card, if you control another Hero, Human Torch deals 1 damage to target
 * opponent.
 * Power-up — {6}{R}: Put three +1/+1 counters on Human Torch. (Activate each power-up ability
 * only once. Reduce the cost by his mana cost if he entered this turn.)
 *
 * The "if you control another Hero" clause is an **intervening-if** (CR 603.4), so it is
 * `triggerCondition` rather than a condition inside the effect: it is checked both when the
 * trigger would fire and again on resolution, and the ability doesn't go on the stack at all when
 * it's false. Wiring it into the effect instead would put a do-nothing trigger on the stack for
 * every card drawn, which is both wrong and noisy.
 *
 * `excludeSelf = true` carries the printed "**another** Hero" — Human Torch is himself a Hero, so
 * without it he would always satisfy his own condition.
 *
 * `{6}{R}` − `{2}{R}` = `{4}`.
 */
val HumanTorchJohnnyStorm = card("Human Torch, Johnny Storm") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Human Hero"
    oracleText = "Flying\n" +
        "Whenever you draw a card, if you control another Hero, Human Torch deals 1 damage to " +
        "target opponent.\n" +
        "Power-up — {6}{R}: Put three +1/+1 counters on Human Torch. (Activate each power-up " +
        "ability only once. Reduce the cost by his mana cost if he entered this turn.)"
    power = 2
    toughness = 2

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.YouDraw
        triggerCondition = Conditions.YouControl(
            GameObjectFilter.Any.withSubtype(Subtype.HERO.value),
            excludeSelf = true
        )
        target = Targets.Opponent
        effect = Effects.DealDamage(1, EffectTarget.ContextTarget(0))
        description = "Whenever you draw a card, if you control another Hero, Human Torch deals " +
            "1 damage to target opponent."
    }

    activatedAbility {
        isPowerUp = true
        cost = Costs.Mana("{6}{R}")
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 3, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "136"
        artist = "Alexander Skripnikov"
        imageUri = "https://cards.scryfall.io/normal/front/8/f/8f8659f6-a793-4edc-8401-d9126840c1c2.jpg?1783902929"
    }
}
