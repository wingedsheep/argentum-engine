package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlockedBy
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * MacCready, Lamplight Mayor
 * {W}{B}
 * Legendary Creature — Human Advisor
 * 1/3
 *
 * Whenever a creature you control with power 2 or less attacks, it gains skulk until end of turn.
 * (It can't be blocked by creatures with greater power.)
 * Whenever a creature with power 4 or greater attacks you, its controller loses 2 life and you gain
 * 2 life.
 *
 * Skulk's whole rules meaning (CR 702.118b) is the blocking restriction its reminder text states, so
 * the grant is that restriction: `CantBeBlockedBy(powerGreaterThanEntity(Self))` handed to the
 * attacker until end of turn. The block check evaluates a granted restriction with the *attacker*
 * as its source, so "greater" compares each blocker against the attacker's power at declare-blockers
 * — skulk's live comparison (Prehistoric Pet prints the same restriction).
 *
 * Both power checks happen only as the abilities trigger (rulings): the conditions are trigger
 * filters, never re-checked on resolution, and "its controller" walks the triggering entity's
 * last-known controller if the attacker is gone by then.
 */
val MacCreadyLamplightMayor = card("MacCready, Lamplight Mayor") {
    manaCost = "{W}{B}"
    typeLine = "Legendary Creature — Human Advisor"
    power = 1
    toughness = 3
    oracleText = "Whenever a creature you control with power 2 or less attacks, it gains skulk until end of turn. " +
        "(It can't be blocked by creatures with greater power.)\n" +
        "Whenever a creature with power 4 or greater attacks you, its controller loses 2 life and you gain 2 life."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl().powerAtMost(2)).attacks()
        effect = Effects.GrantStaticAbility(
            CantBeBlockedBy(GameObjectFilter.Creature.powerGreaterThanEntity(EffectTarget.Self)),
            EffectTarget.TriggeringEntity
        )
        description = "Whenever a creature you control with power 2 or less attacks, it gains skulk until end of turn."
    }

    triggeredAbility {
        trigger = Triggers.a(
            GameObjectFilter.Creature.powerAtLeast(4).attackingPlayer(EffectTarget.Controller)
        ).attacks()
        effect = Effects.LoseLife(2, EffectTarget.ControllerOfTriggeringEntity) then Effects.GainLife(2)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "108"
        artist = "Stephen Stark"
        imageUri = "https://cards.scryfall.io/normal/front/0/d/0d2251ce-2d5b-4d0d-8dc9-44f6b40aa7cf.jpg?1783912387"
        ruling("2024-03-08", "The power of your attacking creature is checked only when MacCready’s first ability triggers. Once it triggers, the creature will gain skulk until end of turn when the ability resolves, regardless of whether or not its power is still 2 or less at that time.")
        ruling("2024-03-08", "Similarly, the power of the creature attacking you is checked only when MacCready’s second ability triggers. Once it triggers, the creature’s controller will lose 2 life and you’ll gain 2 life when the ability resolves, regardless of whether or not its power is still 4 or greater at that time or whether or not it’s still on the battlefield.")
    }
}
