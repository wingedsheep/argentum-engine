package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.PreventionSourceFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Power Leak
 * {1}{U}
 * Enchantment — Aura
 * Enchant enchantment
 * At the beginning of the upkeep of enchanted enchantment's controller, that player may pay any
 * amount of mana. This Aura deals 2 damage to that player. Prevent X of that damage, where X is the
 * amount of mana that player paid this way.
 *
 * Modelling: the step belongs to the enchanted enchantment's controller but the trigger stays the
 * Aura's (`Triggers.attached`); "that player" is the triggering player, who also chooses and pays X
 * (`MayPayX` with that player as decision-maker). The paid X becomes a real prevention shield —
 * so "damage can't be prevented" beats it — covering only the next instance from this Aura to that
 * player, installed before the 2 damage and spent by it, so overpaying never lingers.
 */
val PowerLeak = card("Power Leak") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant enchantment\nAt the beginning of the upkeep of enchanted enchantment's controller, " +
        "that player may pay any amount of mana. This Aura deals 2 damage to that player. Prevent X of " +
        "that damage, where X is the amount of mana that player paid this way."
    auraTarget = TargetObject(filter = TargetFilter.Enchantment)

    triggeredAbility {
        val thatPlayer = EffectTarget.PlayerRef(Player.TriggeringPlayer)
        trigger = Triggers.attached.beginningOf(Step.UPKEEP)
        effect = Effects.MayPayX(
            then = Effects.PreventDamage(
                target = thatPlayer,
                sources = PreventionSourceFilter.Specific(EffectTarget.Self),
                amount = DynamicAmounts.xValue(),
                nextInstanceOnly = true
            ),
            decisionMaker = thatPlayer
        ) then Effects.DealDamage(2, thatPlayer)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "71"
        artist = "Drew Tucker"
        imageUri = "https://cards.scryfall.io/normal/front/c/c/ccc982b6-35b2-4e33-ace2-86cb79123e4f.jpg?1783948702"
    }
}
