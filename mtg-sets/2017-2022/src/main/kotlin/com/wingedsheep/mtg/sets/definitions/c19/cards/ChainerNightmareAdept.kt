package com.wingedsheep.mtg.sets.definitions.c19.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.MayCastFromGraveyard
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Chainer, Nightmare Adept
 * {2}{B}{R}
 * Legendary Creature — Human Minion
 * 3/2
 * Discard a card: You may cast a creature spell from your graveyard this turn. Activate only once
 * each turn.
 * Whenever a nontoken creature you control enters, if you didn't cast it from your hand, it gains
 * haste until your next turn.
 *
 * The first ability creates one permission, not a standing one: each resolution lets you cast one
 * creature spell from your graveyard later in the turn, and the permission outlives Chainer. So it
 * is a one-shot (`Duration.NextUse`) `MayCastFromGraveyard` grant held by the controller — the
 * cast that uses it removes it, and an unused one lapses in the cleanup step. Two activations in a
 * turn (Chainer left and came back) are two grants, so two casts.
 *
 * The haste trigger's "if you didn't cast it from your hand" also holds for a creature that wasn't
 * cast at all — reanimated, flickered, or put onto the battlefield from your hand — and for Chainer
 * itself entering that way.
 */
val ChainerNightmareAdept = card("Chainer, Nightmare Adept") {
    manaCost = "{2}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Legendary Creature — Human Minion"
    oracleText = "Discard a card: You may cast a creature spell from your graveyard this turn. " +
        "Activate only once each turn.\n" +
        "Whenever a nontoken creature you control enters, if you didn't cast it from your hand, " +
        "it gains haste until your next turn."
    power = 3
    toughness = 2

    activatedAbility {
        cost = Costs.DiscardCard
        restrictions = listOf(ActivationRestriction.OncePerTurn)
        effect = Effects.GrantStaticAbility(
            MayCastFromGraveyard(filter = GameObjectFilter.Creature),
            EffectTarget.Controller,
            Duration.NextUse("you cast a creature spell from your graveyard")
        )
        description = "Discard a card: You may cast a creature spell from your graveyard this turn."
    }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.nontoken().youControl()).enters()
        interveningIf = Conditions.Not(Conditions.TriggeringEntityWasCastFrom(Zone.HAND))
        effect = Effects.GrantKeyword(Keyword.HASTE, EffectTarget.TriggeringEntity, Duration.UntilYourNextTurn)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "39"
        artist = "Steve Prescott"
        imageUri = "https://cards.scryfall.io/normal/front/d/7/d7b0a8f8-d368-45e6-a546-984e7b1913e0.jpg?1783932800"
    }
}
