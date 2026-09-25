package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Kashi-Tribe Warriors
 * {3}{G}{G}
 * Creature — Snake Warrior
 * 2/4
 * Whenever this creature deals combat damage to a creature, tap that creature and it doesn't untap
 * during its controller's next untap step.
 *
 * The damaged creature is the trigger's [EffectTarget.TriggeringEntity]. The untap lock is a
 * `DOESNT_UNTAP` grant bounded by [Duration.UntilAfterAffectedControllersNextUntap], keyed to the
 * affected creature's controller — the same shape as `drk/cards/BarlsCage.kt`.
 */
val KashiTribeWarriors = card("Kashi-Tribe Warriors") {
    manaCost = "{3}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Snake Warrior"
    power = 2
    toughness = 4
    oracleText = "Whenever this creature deals combat damage to a creature, tap that creature and it doesn't untap during its controller's next untap step."

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
        collectorNumber = "221"
        artist = "Stephen Tappin"
        flavorText = "The orochi and the monks had always had an unspoken agreement: live and let live. But when the kami began raging, some warrior youths began questioning that agreement."
        imageUri = "https://cards.scryfall.io/normal/front/7/c/7cd4df4a-fbab-4598-ad8e-b24ed0bb4497.jpg?1783944287"
    }
}
