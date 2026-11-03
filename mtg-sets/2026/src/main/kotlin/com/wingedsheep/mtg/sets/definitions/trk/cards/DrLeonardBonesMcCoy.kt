package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerExpiry
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Dr. Leonard "Bones" McCoy
 * {1}{G}{G}
 * Legendary Creature — Human Doctor
 * 3/2
 * Flash
 * When Dr. McCoy enters, choose one —
 * • Return target non-Doctor permanent card from your graveyard to your hand.
 * • Choose up to one other target creature. When it dies this turn, return that card to the
 *   battlefield under its owner's control with a +1/+1 counter on it at the beginning of the next
 *   end step.
 *
 * Mode two nests two delayed triggers: A Good Day to Die's watched, once-only "when it dies this
 * turn", whose effect schedules The Locust God's "at the beginning of the next end step" return of
 * the triggering card from the graveyard. The scheduling bakes the dead card's id, so the return
 * does nothing if the card has left the graveyard (or was a token) by then; a move onto the
 * battlefield is under the owner's control.
 */
val DrLeonardBonesMcCoy = card("Dr. Leonard \"Bones\" McCoy") {
    manaCost = "{1}{G}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Human Doctor"
    power = 3
    toughness = 2
    oracleText = "Flash\n" +
        "When Dr. McCoy enters, choose one —\n" +
        "• Return target non-Doctor permanent card from your graveyard to your hand.\n" +
        "• Choose up to one other target creature. When it dies this turn, return that card to the " +
        "battlefield under its owner's control with a +1/+1 counter on it at the beginning of the next end step."

    keywords(Keyword.FLASH)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = ModalEffect.chooseOne(
            mode("Return target non-Doctor permanent card from your graveyard to your hand.") {
                val returned = target(
                    TargetFilter(
                        GameObjectFilter.Permanent.ownedByYou().notSubtype(Subtype("Doctor")),
                        zone = Zone.GRAVEYARD,
                    )
                )
                effect = Effects.Move(returned, Zone.HAND, fromZone = Zone.GRAVEYARD)
            },
            mode(
                "Choose up to one other target creature. When it dies this turn, return that card to the " +
                    "battlefield under its owner's control with a +1/+1 counter on it at the beginning of the next end step."
            ) {
                val creature = target(TargetFilter.OtherCreature, optional = true)
                effect = Effects.CreateDelayedTrigger(
                    effect = Effects.CreateDelayedTrigger(
                        step = Step.END,
                        effect = Effects.Move(
                            target = EffectTarget.TriggeringEntity,
                            destination = Zone.BATTLEFIELD,
                            fromZone = Zone.GRAVEYARD,
                            addCounterType = CounterType.PLUS_ONE_PLUS_ONE,
                        ),
                    ),
                    trigger = Triggers.self.dies(),
                    watchedTarget = creature,
                    fireOnce = true,
                    expiry = DelayedTriggerExpiry.EndOfTurn,
                )
            },
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "197"
        artist = "Sam Kennedy"
        imageUri = "https://cards.scryfall.io/normal/front/d/8/d8593bf7-5248-4f4a-bf8b-9816d2f76c6b.jpg?1793187127"
    }
}
