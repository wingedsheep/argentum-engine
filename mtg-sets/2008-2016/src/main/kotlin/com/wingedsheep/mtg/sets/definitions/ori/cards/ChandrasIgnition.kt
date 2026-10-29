package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Chandra's Ignition — Magic Origins #137
 * {3}{R}{R} · Sorcery
 *
 * Target creature you control deals damage equal to its power to each other creature and each
 * opponent.
 *
 * Nibelheim Aflame's shape — `ForEachInGroup` over every creature other than the target, each
 * dealt `powerOf(target)` with the target as the damage source — followed by the same amount to
 * each opponent, again sourced from the target creature (rulings: the creature is the source).
 */
val ChandrasIgnition = card("Chandra's Ignition") {
    manaCost = "{3}{R}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Target creature you control deals damage equal to its power to each other creature " +
        "and each opponent."

    spell {
        val chosen = target(TargetFilter.CreatureYouControl)
        effect = Effects.ForEachInGroup(
            filter = GroupFilter(GameObjectFilter.Creature).otherThanTarget(),
            effect = Effects.DealDamage(
                amount = DynamicAmounts.powerOf(chosen),
                target = EffectTarget.IterationEntity,
                damageSource = chosen,
            ),
        ) then
            Effects.DealDamage(
                amount = DynamicAmounts.powerOf(chosen),
                target = EffectTarget.PlayerRef(Player.EachOpponent),
                damageSource = chosen,
            )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "137"
        artist = "Eric Deschamps"
        flavorText = "In the moment before her execution, she realized what it meant to be a pyromancer, " +
            "to be alive, to be Chandra."
        imageUri = "https://cards.scryfall.io/normal/front/7/d/7d4c90de-49aa-43ed-a18a-f7f96268e5eb.jpg?1783938332"
        ruling(
            "2015-06-22",
            "The creature is the source of the damage, not Chandra's Ignition. For example, Chandra's Ignition " +
                "can have a white creature deal damage to a creature with protection from red."
        )
        ruling(
            "2015-06-22",
            "Use the power of the target creature as Chandra's Ignition resolves to determine how much damage " +
                "it deals to each other creature and each opponent."
        )
    }
}
