package com.wingedsheep.engine.state.components.identity

import com.wingedsheep.engine.state.Component
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.model.EntityId
import kotlinx.serialization.Serializable

/**
 * Card-intrinsic marker: this card has Madness (CR 702.35) for [cost].
 *
 * Stamped once by [com.wingedsheep.engine.core.CardEntityFactory] from the card's
 * [com.wingedsheep.sdk.scripting.KeywordAbility.Madness], so it rides the entity in every zone —
 * which is what the CR 702.35a static ability needs, since it functions while the card is in a
 * player's *hand* and is read by the zone-change replacement check as the card is being discarded.
 *
 * Carrying the cost here (rather than looking the card definition up again) is what lets the
 * discard path stamp the exiled card with its madness cost without a registry lookup.
 */
@Serializable
data class MadnessComponent(
    val cost: ManaCost
) : Component

/**
 * Marks a card that is in exile *because* the madness replacement redirected a discard there
 * (CR 702.35a). Distinguishes "discarded into exile" from every other way a card reaches exile, so
 * only the former gets the CR 702.35a triggered ability and the fixed-madness-cost cast permission.
 *
 * Applied alongside a [PlayWithFixedAlternativeManaCostComponent] carrying the madness cost, and
 * both are stripped together when the card leaves exile — whether it was cast, put into the
 * graveyard by the trigger, or moved by something else entirely. Stripping matters: a lingering
 * fixed alternative cost would otherwise silently re-price a later flashback-style cast from the
 * graveyard.
 *
 * @param ownerId The card's owner — the player the madness cast is offered to (CR 702.35a says
 *   "its owner may cast it", not the player who caused the discard).
 */
@Serializable
data class MadnessExiledComponent(
    val ownerId: EntityId
) : Component

/**
 * Battlefield permanent marker: this permanent grants madness to cards its controller owns that
 * match one of [filters] and aren't on the battlefield, at each card's own mana cost (Falkenrath
 * Gorger). Baked from [com.wingedsheep.sdk.scripting.GrantMadnessToOwnedCards] by
 * [com.wingedsheep.engine.mechanics.layers.StaticAbilityHandler].
 *
 * A component rather than a card-definition lookup for the same reason the ward and hexproof
 * suppressors are: the discard-replacement check walks the battlefield with no [CardRegistry] in
 * hand, so the grant has to be readable straight off the permanent. Read via
 * [com.wingedsheep.engine.mechanics.MadnessGrants].
 */
@Serializable
data class GrantsMadnessToOwnedCardsComponent(
    val filters: List<com.wingedsheep.sdk.scripting.GameObjectFilter>
) : Component
