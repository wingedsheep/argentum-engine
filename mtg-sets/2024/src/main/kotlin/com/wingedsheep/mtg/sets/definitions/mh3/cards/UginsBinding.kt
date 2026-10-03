package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ugin's Binding — Modern Horizons 3 #76 (mythic)
 * {2}{U} · Instant
 *
 * Devoid
 * Return target nonland permanent you don't control to its owner's hand.
 * Whenever you cast a colorless spell with mana value 7 or greater, you may exile this card from
 * your graveyard. When you do, return each nonland permanent you don't control to its owner's hand.
 *
 * "You don't control" is `Not(ControlledByYou)` rather than `opponentControls()` — they differ in
 * Two-Headed Giant, where a teammate's permanent is one you don't control.
 *
 * The graveyard ability is zone-scoped with `triggerZone = Zone.GRAVEYARD` (it triggers only if the
 * card is in your graveyard as the spell is cast). The exile is the optional action of a
 * reflexive trigger (CR 603.12): the mass bounce is a separate ability that resolves before the
 * colorless spell. The `SourceInZone(GRAVEYARD)` gate makes the may-question vanish if Ugin's
 * Binding left the graveyard in response — it can't be exiled then, so "when you do" never fires
 * (a `fromZone` move that skips reports success, which would otherwise arm the payoff).
 */
private val nonlandYouDontControl = GameObjectFilter.NonlandPermanent.withControllerPredicate(
    ControllerPredicate.Not(ControllerPredicate.ControlledByYou)
)

val UginsBinding = card("Ugin's Binding") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Devoid (This card has no color.)\n" +
        "Return target nonland permanent you don't control to its owner's hand.\n" +
        "Whenever you cast a colorless spell with mana value 7 or greater, you may exile this card " +
        "from your graveyard. When you do, return each nonland permanent you don't control to its " +
        "owner's hand."

    keywords(Keyword.DEVOID)

    spell {
        val permanent = target(TargetFilter(nonlandYouDontControl))
        effect = Effects.ReturnToHand(permanent)
    }

    triggeredAbility {
        trigger = Triggers.you.casts(
            GameObjectFilter.Any.withCardPredicate(CardPredicate.IsColorless).manaValueAtLeast(7)
        )
        triggerZone = Zone.GRAVEYARD
        effect = Effects.If(
            Conditions.SourceInZone(Zone.GRAVEYARD),
            Effects.ReflexiveTrigger(
                action = Effects.Exile(EffectTarget.Self, fromZone = Zone.GRAVEYARD),
                reflexiveEffect = Patterns.Group.returnAllToHand(GroupFilter(nonlandYouDontControl)),
                optional = true,
                descriptionOverride = "You may exile this card from your graveyard. When you do, " +
                    "return each nonland permanent you don't control to its owner's hand.",
            ),
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "76"
        artist = "Drew Baker"
        imageUri = "https://cards.scryfall.io/normal/front/b/e/be13786a-f967-456b-bbc6-f4312467a827.jpg?1783911286"
        ruling(
            "2024-06-07",
            "The last ability triggers only if Ugin's Binding is in your graveyard as you cast the colorless " +
                "spell. That ability will resolve before the colorless spell does. If the colorless spell is " +
                "countered or otherwise leaves the stack in response to Ugin's Binding's triggered ability, " +
                "the ability will still resolve as normal."
        )
    }
}
