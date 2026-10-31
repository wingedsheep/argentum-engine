package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithDynamicCounters
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Voracious Hydra
 * {X}{G}{G}
 * Creature — Hydra
 * 0/1
 *
 * Trample
 * This creature enters with X +1/+1 counters on it.
 * When this creature enters, choose one —
 * • Double the number of +1/+1 counters on this creature.
 * • This creature fights target creature you don't control.
 *
 * The X counters are an entry replacement ([EntersWithDynamicCounters]), so they are already on
 * the Hydra when the ETB modal trigger resolves; mode 1 doubles them with [Effects.DoubleCounters].
 * Mode 2 is a self-fight (the Sawblade Slinger idiom) against a creature not controlled by you.
 */
val VoraciousHydra = card("Voracious Hydra") {
    manaCost = "{X}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Hydra"
    power = 0
    toughness = 1
    oracleText = "Trample\n" +
        "This creature enters with X +1/+1 counters on it.\n" +
        "When this creature enters, choose one —\n" +
        "• Double the number of +1/+1 counters on this creature.\n" +
        "• This creature fights target creature you don't control."

    keywords(Keyword.TRAMPLE)

    replacementEffect(EntersWithDynamicCounters(count = DynamicAmounts.xValue()))

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Modal(
            modes = listOf(
                mode("Double the number of +1/+1 counters on this creature") {
                    effect = Effects.DoubleCounters(CounterType.PLUS_ONE_PLUS_ONE, EffectTarget.Self)
                },
                mode("This creature fights target creature you don't control") {
                    val creature = target(
                        TargetFilter(
                            GameObjectFilter.Creature.copy(
                                controllerPredicate = ControllerPredicate.Not(ControllerPredicate.ControlledByYou)
                            )
                        )
                    )
                    effect = Effects.Fight(EffectTarget.Self, creature)
                }
            ),
            countsAsModalSpell = false
        )
        description = "When this creature enters, choose one — Double the number of +1/+1 counters on " +
            "this creature, or this creature fights target creature you don't control."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "200"
        artist = "Wayne Reynolds"
        imageUri = "https://cards.scryfall.io/normal/front/7/4/74bbe44f-864a-446c-bfc9-5f5188e663b8.jpg?1783932954"
        ruling("2019-07-12", "Because fighting isn't combat damage, trample has no effect during the fight.")
        ruling(
            "2019-07-12",
            "If the target of Voracious Hydra's second mode isn't a legal target as the ability resolves, or if " +
                "Voracious Hydra has left the battlefield, neither creature will deal or be dealt damage. The " +
                "ability won't change to the first mode if the target is illegal."
        )
    }
}
