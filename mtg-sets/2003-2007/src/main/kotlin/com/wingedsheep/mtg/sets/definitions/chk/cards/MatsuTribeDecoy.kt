package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Matsu-Tribe Decoy
 * {2}{G}
 * Creature — Snake Warrior
 * 1/3
 * {2}{G}: Target creature blocks this creature this turn if able.
 * Whenever this creature deals combat damage to a creature, tap that creature and it doesn't untap
 * during its controller's next untap step.
 *
 * The damaged creature is the trigger's [EffectTarget.TriggeringEntity]. The untap lock is a
 * `DOESNT_UNTAP` grant bounded by [Duration.UntilAfterAffectedControllersNextUntap], keyed to the
 * affected creature's controller — the same shape as `drk/cards/BarlsCage.kt`.
 */
val MatsuTribeDecoy = card("Matsu-Tribe Decoy") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Snake Warrior"
    power = 1
    toughness = 3
    oracleText = "{2}{G}: Target creature blocks this creature this turn if able.\nWhenever this creature deals combat damage to a creature, tap that creature and it doesn't untap during its controller's next untap step."

    activatedAbility {
        val creature = target(TargetFilter.Creature)
        cost = Costs.Mana("{2}{G}")
        effect = Effects.ForceBlock(creature)
    }

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyCreature)
        effect = Effects.Tap(EffectTarget.TriggeringEntity) then
            Effects.GrantKeyword(
                AbilityFlag.DOESNT_UNTAP,
                EffectTarget.TriggeringEntity,
                Duration.UntilAfterAffectedControllersNextUntap,
            )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "227"
        artist = "Alan Pollack"
        imageUri = "https://cards.scryfall.io/normal/front/1/4/14bf9bec-9a5d-48a6-941b-95e1633b9484.jpg?1783944286"
    }
}
