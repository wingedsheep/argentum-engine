package com.wingedsheep.mtg.sets.definitions.rix.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Form of the Dinosaur
 * {4}{R}{R}
 * Enchantment
 * When this enchantment enters, your life total becomes 15.
 * At the beginning of your upkeep, this enchantment deals 15 damage to target creature an opponent
 * controls and that creature deals damage equal to its power to you.
 *
 * The upkeep trigger is Tracker's two-sentence "fight in all but name": two ordinary
 * [Effects.DealDamage] steps, the second naming the target as its `damageSource` so prevention and
 * "damage dealt by" triggers see the creature as the dealer. No SBA pass runs between the steps, so
 * a creature killed by the 15 damage still deals its damage back, reading its power at resolution.
 */
val FormOfTheDinosaur = card("Form of the Dinosaur") {
    manaCost = "{4}{R}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment"
    oracleText = "When this enchantment enters, your life total becomes 15.\n" +
        "At the beginning of your upkeep, this enchantment deals 15 damage to target creature an " +
        "opponent controls and that creature deals damage equal to its power to you."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.SetLifeTotal(15)
        description = "When this enchantment enters, your life total becomes 15."
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        val prey = target(TargetFilter.CreatureOpponentControls)
        effect = Effects.DealDamage(15, prey, damageSource = EffectTarget.Self) then
            Effects.DealDamage(
                DynamicAmounts.powerOf(prey),
                EffectTarget.Controller,
                damageSource = prey,
            )
        description = "At the beginning of your upkeep, this enchantment deals 15 damage to target " +
            "creature an opponent controls and that creature deals damage equal to its power to you."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "103"
        artist = "Daarken"
        imageUri = "https://cards.scryfall.io/normal/front/4/1/41e4fe86-281d-4174-bb72-ac5bb9560b7e.jpg?1783935299"
    }
}
