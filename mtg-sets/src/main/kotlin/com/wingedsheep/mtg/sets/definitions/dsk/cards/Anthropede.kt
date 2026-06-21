package com.wingedsheep.mtg.sets.definitions.dsk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ChooseActionEffect
import com.wingedsheep.sdk.scripting.effects.EffectChoice
import com.wingedsheep.sdk.scripting.effects.FeasibilityCheck
import com.wingedsheep.sdk.scripting.effects.PayManaCostEffect
import com.wingedsheep.sdk.scripting.effects.ReflexiveTriggerEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent
import com.wingedsheep.sdk.model.Rarity

/**
 * Anthropede
 * {3}{G}
 * Creature — Insect
 * 3/4
 * Reach
 * When this creature enters, you may discard a card or pay {2}. When you do, destroy target Room.
 *
 * The enters trigger is an optional "you may [discard a card or pay {2}]" action; if taken, a
 * reflexive trigger ("When you do, …") goes on the stack and targets a Room (CR 603.2c — the
 * target is chosen as the reflexive trigger is put on the stack, not when the enters trigger
 * resolves). Same shape as Nimble Hobbit (sacrifice-or-pay → reflexive tap). Reuses
 * [ChooseActionEffect] for the two-way cost choice and [ReflexiveTriggerEffect] for the deferred
 * targeted destroy.
 */
val Anthropede = card("Anthropede") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Insect"
    oracleText = "Reach\nWhen this creature enters, you may discard a card or pay {2}. " +
        "When you do, destroy target Room."
    power = 3
    toughness = 4

    keywords(Keyword.REACH)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = ReflexiveTriggerEffect(
            // "you may discard a card or pay {2}"
            action = ChooseActionEffect(
                choices = listOf(
                    EffectChoice(
                        label = "Discard a card",
                        effect = Patterns.Hand.discardCards(1),
                        feasibilityCheck = FeasibilityCheck.HasCardsInZone(Zone.HAND)
                    ),
                    EffectChoice(
                        label = "Pay {2}",
                        effect = PayManaCostEffect(ManaCost.parse("{2}"))
                    )
                )
            ),
            optional = true,
            // "When you do, destroy target Room."
            reflexiveEffect = Effects.Destroy(EffectTarget.ContextTarget(0)),
            reflexiveTargetRequirements = listOf(
                TargetPermanent(filter = TargetFilter(GameObjectFilter.Permanent.withSubtype("Room")))
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "167"
        artist = "Loïc Canavaggia"
        flavorText = "The touch of its many hands is almost gentle, at first. " +
            "But then the grip tightens, and the mandibles dig in."
        imageUri = "https://cards.scryfall.io/normal/front/5/1/51216ab0-9806-4faa-afbd-143e95dc255b.jpg?1726286480"
    }
}
