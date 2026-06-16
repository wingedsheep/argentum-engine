package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.events.DamageType
import com.wingedsheep.sdk.scripting.events.RecipientFilter
import com.wingedsheep.sdk.scripting.values.ContextPropertyKey
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Fall of Cair Andros
 * {2}{R}
 * Enchantment
 *
 * Whenever a creature an opponent controls is dealt excess noncombat damage, amass Orcs
 * X, where X is that excess damage.
 *
 * {7}{R}: This enchantment deals 7 damage to target creature.
 *
 * Composes the Gap 12 excess-damage trigger primitive (`requireExcess = true` on the
 * deals-damage trigger, with `ContextPropertyKey.TRIGGER_EXCESS_DAMAGE_AMOUNT` exposing
 * the excess to the amass amount).
 */
val FallOfCairAndros = card("Fall of Cair Andros") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment"
    oracleText = "Whenever a creature an opponent controls is dealt excess noncombat damage, amass Orcs X, where X is that excess damage. (Put X +1/+1 counters on an Army you control. It's also an Orc. If you don't control an Army, create a 0/0 black Orc Army creature token first.)\n{7}{R}: This enchantment deals 7 damage to target creature."

    triggeredAbility {
        trigger = Triggers.dealsDamage(
            damageType = DamageType.NonCombat,
            recipient = RecipientFilter.CreatureOpponentControls,
            binding = TriggerBinding.ANY,
            requireExcess = true,
        )
        effect = Effects.Amass(
            DynamicAmount.ContextProperty(ContextPropertyKey.TRIGGER_EXCESS_DAMAGE_AMOUNT),
            "Orc"
        )
    }

    activatedAbility {
        cost = Costs.Mana("{7}{R}")
        val creature = target("creature", Targets.Creature)
        effect = Effects.DealDamage(7, creature)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "124"
        artist = "Shahab Alizadeh"
        imageUri = "https://cards.scryfall.io/normal/front/3/5/354b4623-50a0-41f1-ad1e-7dd6ac3852df.jpg?1686968903"
        ruling("2023-06-16", "Excess damage has been dealt to a creature if the damage dealt to it is greater than lethal damage. Usually, this means damage greater than its toughness, although damage already marked on the creature is taken into account.")
        ruling("2023-06-16", "Some cards refer to the \"amassed Army.\" That means the Army creature you chose to receive counters, even if no counters were placed on it for some reason.")
    }
}
