package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantAttack
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Captured by the Consulate
 * {3}{W}
 * Enchantment — Aura
 * Enchant creature you don't control
 * Enchanted creature can't attack.
 * Whenever an opponent casts a spell, if it has a single target, change the target to enchanted
 * creature if able.
 *
 * The cast trigger redirects the spell it fired on ([EffectTarget.TriggeringEntity]) to the fixed
 * new target [EffectTarget.EnchantedCreature]; "if able" is the fixed-target redirect's own rule —
 * the target changes only when the enchanted creature is a legal target for that spell. "If it has
 * a single target" is an intervening if, checked as the spell is cast and again on resolution.
 */
val CapturedByTheConsulate = card("Captured by the Consulate") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature you don't control\n" +
        "Enchanted creature can't attack.\n" +
        "Whenever an opponent casts a spell, if it has a single target, change the target to " +
        "enchanted creature if able."

    auraTarget = TargetObject(filter = TargetFilter.Creature.opponentControls())

    staticAbility {
        ability = CantAttack(filter = GroupFilter.attachedCreature())
    }

    triggeredAbility {
        trigger = Triggers.anOpponent.casts()
        interveningIf = Conditions.TriggeringSpellHasSingleTarget
        effect = Effects.ChangeTarget(to = EffectTarget.EnchantedCreature, spell = EffectTarget.TriggeringEntity)
        description = "Whenever an opponent casts a spell, if it has a single target, change the target " +
            "to enchanted creature if able."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "8"
        artist = "Tyler Jacobson"
        imageUri = "https://cards.scryfall.io/normal/front/e/0/e0c20018-4446-4c2b-bdc5-53fcf1fbc3bf.jpg?1783937235"
        ruling("2016-09-20", "When you attach Captured by the Consulate to an opponent's creature, you still control Captured by the Consulate. Its last ability triggers whenever your opponents cast a spell with a single target, not when an opponent of the creature's controller casts a spell with a single target.")
        ruling("2016-09-20", "When a player casts an Aura spell, it targets the creature that it will enchant and may cause Captured by the Consulate's last ability to trigger.")
        ruling("2016-09-20", "If a spell has multiple targets, it doesn't have a single target even if the same object is chosen for each of those targets.")
        ruling("2016-09-20", "If the spell can't target the enchanted creature, its target remains unchanged.")
        ruling("2016-09-20", "If there is more than one Captured by the Consulate on the battlefield, the triggered ability of each one will change the target if able. The last one to resolve determines what that spell finally targets.")
    }
}
