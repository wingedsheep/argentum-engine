package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Kashi-Tribe Reaver
 * {3}{G}
 * Creature — Snake Warrior
 * 3/2
 * Whenever this creature deals combat damage to a creature, tap that creature and it doesn't untap
 * during its controller's next untap step.
 * {1}{G}: Regenerate this creature.
 *
 * The damaged creature is the trigger's [EffectTarget.TriggeringEntity]. The untap lock is a
 * `DOESNT_UNTAP` grant bounded by [Duration.UntilAfterAffectedControllersNextUntap], keyed to the
 * affected creature's controller — the same shape as `drk/cards/BarlsCage.kt`.
 */
val KashiTribeReaver = card("Kashi-Tribe Reaver") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Snake Warrior"
    power = 3
    toughness = 2
    oracleText = "Whenever this creature deals combat damage to a creature, tap that creature and it doesn't untap during its controller's next untap step.\n{1}{G}: Regenerate this creature."

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyCreature)
        effect = Effects.Tap(EffectTarget.TriggeringEntity) then
            Effects.GrantKeyword(
                AbilityFlag.DOESNT_UNTAP,
                EffectTarget.TriggeringEntity,
                Duration.UntilAfterAffectedControllersNextUntap,
            )
    }

    activatedAbility {
        cost = Costs.Mana("{1}{G}")
        effect = Effects.Regenerate(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "220"
        artist = "Anthony S. Waters"
        imageUri = "https://cards.scryfall.io/normal/front/3/d/3d417f90-10e9-4c41-ac3b-1a861147d69e.jpg?1783944287"
    }
}
