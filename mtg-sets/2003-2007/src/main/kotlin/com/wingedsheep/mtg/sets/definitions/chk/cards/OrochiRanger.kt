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
 * Orochi Ranger
 * {1}{G}
 * Creature — Snake Warrior Ranger
 * 2/1
 * Whenever this creature deals combat damage to a creature, tap that creature and it doesn't untap
 * during its controller's next untap step.
 *
 * The damaged creature is the trigger's [EffectTarget.TriggeringEntity]. The untap lock is a
 * `DOESNT_UNTAP` grant bounded by [Duration.UntilAfterAffectedControllersNextUntap], keyed to the
 * affected creature's controller — the same shape as `drk/cards/BarlsCage.kt`.
 */
val OrochiRanger = card("Orochi Ranger") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Snake Warrior Ranger"
    power = 2
    toughness = 1
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
        collectorNumber = "235"
        artist = "Greg Hildebrandt"
        flavorText = "\"The young come to me, confused. They have been taught to respect the kami, and now they must fight them? I do not know what to say.\"\n—Sachi, to her father"
        imageUri = "https://cards.scryfall.io/normal/front/e/8/e8dc216f-4447-4370-b31a-18304507669b.jpg?1783944283"
    }
}
