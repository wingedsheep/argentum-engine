package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.RegenerateEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.ContextPropertyKey
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Flayed Nim — Mirrodin #65
 * {3}{B} · Creature — Skeleton · 2/2
 *
 * Whenever this creature deals combat damage to a creature, that creature's controller loses
 * that much life.
 * {2}{B}: Regenerate this creature.
 *
 * The damage rider reads both halves of its own trigger payload rather than the Nim's power:
 * `TRIGGER_DAMAGE_AMOUNT` is the damage actually dealt in that combat-damage event, and
 * [EffectTarget.ControllerOfTriggeringEntity] is the controller of the *damaged* creature —
 * `DamageDealtEvent` sets the triggering entity to the recipient, so "that creature's
 * controller" resolves without a target being declared.
 *
 * Reading the event rather than the source's power is what makes the edge cases fall out
 * right: a Nim pumped after blockers are declared drains for what it actually dealt, damage
 * reduced by a protection/prevention shield drains for the reduced amount (prevented to zero
 * deals no damage, so the trigger never fires at all), and only *combat* damage counts — the
 * shape [Tephraderm][com.wingedsheep.mtg.sets.definitions.ons.cards.Tephraderm] uses on the
 * incoming side.
 *
 * Regeneration is the survival half: a shield taps the Nim and removes it from combat instead
 * of letting it die (CR 701.15), so the same body can trade repeatedly. Note the drain trigger
 * has already fired by then — combat damage is dealt before the lethal-damage state-based
 * action, so a Nim that regenerates after a trade still drained.
 */
val FlayedNim = card("Flayed Nim") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Skeleton"
    power = 2
    toughness = 2
    oracleText = "Whenever this creature deals combat damage to a creature, that creature's " +
        "controller loses that much life.\n" +
        "{2}{B}: Regenerate this creature."

    triggeredAbility {
        trigger = Triggers.DealsCombatDamageToCreature
        effect = Effects.LoseLife(
            amount = DynamicAmount.ContextProperty(ContextPropertyKey.TRIGGER_DAMAGE_AMOUNT),
            target = EffectTarget.ControllerOfTriggeringEntity
        )
        description = "Whenever this creature deals combat damage to a creature, that " +
            "creature's controller loses that much life."
    }

    activatedAbility {
        cost = Costs.Mana("{2}{B}")
        effect = RegenerateEffect(EffectTarget.Self)
        description = "{2}{B}: Regenerate this creature."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "65"
        artist = "Trevor Hairsine"
        imageUri = "https://cards.scryfall.io/normal/front/f/d/fd2381b3-ebf8-4c65-8e89-e089c2e57145.jpg?1783944548"
    }
}
