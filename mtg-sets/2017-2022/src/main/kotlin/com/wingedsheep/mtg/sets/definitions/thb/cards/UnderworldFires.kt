package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Underworld Fires
 * {1}{R}
 * Sorcery
 * Underworld Fires deals 1 damage to each creature and each planeswalker. If a permanent dealt
 * damage this way would die this turn, exile it instead.
 *
 * Draconic Intervention's shape over [GameObjectFilter.CreatureOrPlaneswalker]: per permanent, the
 * damage records how much it actually dealt, and only a permanent that was really dealt damage gets
 * the "exile instead of dying this turn" mark ([Effects.MarkExileOnDeath]) — a permanent whose damage
 * was prevented still dies normally. The mark covers any way of dying for the rest of the turn, not
 * just lethal damage (ruling), and applies to a planeswalker that falls to 0 loyalty too (Fanged
 * Flames). The marks are placed during resolution, before state-based actions see the damage.
 */
val UnderworldFires = card("Underworld Fires") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Underworld Fires deals 1 damage to each creature and each planeswalker. If a permanent " +
        "dealt damage this way would die this turn, exile it instead."

    spell {
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.CreatureOrPlaneswalker),
            Effects.Pipeline {
                val dealt = runStoringNumber {
                    Effects.DealDamage(1, EffectTarget.IterationEntity, damageDealtVariable = it)
                }
                ifThen(Conditions.CompareAmounts(dealt.amount, ComparisonOperator.GT, 0)) {
                    run(Effects.MarkExileOnDeath(EffectTarget.IterationEntity))
                }
            }
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "162"
        artist = "Bayard Wu"
        flavorText = "The Underworld is anything but a resting place."
        imageUri = "https://cards.scryfall.io/normal/front/0/f/0fe616c4-dcb0-4284-ba10-6fbf7cecd217.jpg?1783931542"
        ruling(
            "2020-01-24",
            "Creatures and planeswalkers don't necessarily have to be dealt lethal damage by Underworld Fires " +
                "to be exiled. After being dealt damage, if they would die for any reason that turn, they'll be " +
                "exiled instead."
        )
    }
}
