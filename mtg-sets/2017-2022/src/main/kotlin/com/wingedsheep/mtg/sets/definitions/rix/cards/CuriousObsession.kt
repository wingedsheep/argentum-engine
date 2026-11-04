package com.wingedsheep.mtg.sets.definitions.rix.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantTriggeredAbility
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.effects.SacrificeSelfEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Curious Obsession — Rivals of Ixalan #35 (canonical printing)
 * {U} · Enchantment — Aura
 *
 * Enchant creature
 * Enchanted creature gets +1/+1 and has "Whenever this creature deals combat damage to a player,
 * you may draw a card."
 * At the beginning of your end step, if you didn't attack with a creature this turn, sacrifice
 * this Aura.
 *
 * The draw is a *granted* ability ([GrantTriggeredAbility] over the attached creature, the Instill
 * Furor shape), so "you" is the enchanted creature's controller, not the Aura's.
 *
 * The sacrifice is an intervening "if" on the Aura's own end-step trigger, read off the
 * controller's "attacked this turn" marker ([Conditions.YouAttackedThisTurn]) — per the 2018-01-19
 * ruling, any creature attacking satisfies it, not just the enchanted one (raid-style).
 */
val CuriousObsession = card("Curious Obsession") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature gets +1/+1 and has \"Whenever this creature deals combat damage to a " +
        "player, you may draw a card.\"\n" +
        "At the beginning of your end step, if you didn't attack with a creature this turn, " +
        "sacrifice this Aura."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = ModifyStats(1, 1)
    }

    staticAbility {
        ability = GrantTriggeredAbility(
            ability = TriggeredAbility.create(
                trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer),
                effect = Effects.May(Effects.DrawCards(1)),
                descriptionOverride = "Whenever this creature deals combat damage to a player, " +
                    "you may draw a card.",
            ),
            filter = GroupFilter.attachedCreature(),
        )
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        interveningIf = Conditions.Not(Conditions.YouAttackedThisTurn)
        effect = SacrificeSelfEffect
        description = "At the beginning of your end step, if you didn't attack with a creature " +
            "this turn, sacrifice this Aura."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "35"
        artist = "Daniel Ljunggren"
        imageUri = "https://cards.scryfall.io/normal/front/7/f/7f998c62-30c9-4fc3-a045-5becc04f9e3e.jpg?1783935327"
        ruling(
            "2018-01-19",
            "Curious Obsession's last ability is satisfied if any creature has attacked, similar " +
                "to raid abilities. The creature it enchants doesn't have to have attacked.",
        )
    }
}
