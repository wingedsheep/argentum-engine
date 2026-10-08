package com.wingedsheep.engine.state.components.identity

import com.wingedsheep.engine.state.Component
import com.wingedsheep.sdk.model.EntityId
import kotlinx.serialization.Serializable

/**
 * Marks a **melded permanent** (CR 701.42a) — one object represented by two cards.
 *
 * The host is the meld card whose ability did the melding; its entity *is* the permanent, and its
 * [CardComponent] carries the meld result's characteristics (CR 712.8g, with the two front faces'
 * summed mana value). The other card, [partnerId], is held in no zone while the pair is melded: it
 * is part of the permanent, not a second object, so nothing can target, count or move it on its own.
 *
 * When the permanent leaves the battlefield,
 * [com.wingedsheep.engine.handlers.effects.ZoneTransitionService] turns the host back into
 * [hostFrontCard] and puts the partner card into the same zone (CR 712.21: one permanent leaves the
 * battlefield, two cards are put into the new zone).
 *
 * Deliberately *not* a [DoubleFacedComponent]: meld cards can't be transformed (CR 712.4c) and a
 * melded permanent is never a "transformed permanent" (CR 701.27g), so keeping it out of the
 * double-faced machinery makes every transform path ignore it without a guard of its own.
 */
@Serializable
data class MeldedComponent(
    val partnerId: EntityId,
    val hostFrontCard: CardComponent,
) : Component
