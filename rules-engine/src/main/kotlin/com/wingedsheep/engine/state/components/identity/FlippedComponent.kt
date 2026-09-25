package com.wingedsheep.engine.state.components.identity

import com.wingedsheep.engine.state.Component
import com.wingedsheep.engine.state.ComponentContainer
import kotlinx.serialization.Serializable

/**
 * The flipped status of a flip-card permanent (CR 110.5, 710).
 *
 * While present, the entity's [CardComponent] carries the flip half's name, type line, rules text
 * and P/T — with the card's own mana cost and colour kept (CR 710.1c). [unflippedCard] is the
 * upright [CardComponent] the entity had before it flipped, so
 * [com.wingedsheep.engine.handlers.effects.ZoneTransitionService] can restore it when the permanent
 * leaves the battlefield: a flipped permanent that leaves retains no memory of its status
 * (CR 710.4). Its presence is also what makes flipping one-way — a flipped permanent can't flip
 * again, and nothing un-flips it while it stays on the battlefield.
 */
@Serializable
data class FlippedComponent(
    val unflippedCard: CardComponent,
) : Component

/**
 * The [CardComponent] a copy effect should copy from this object. Flipped is a status, and status
 * isn't copied (CR 707.2) — a copy of a flipped permanent is its upright half, able to flip on its
 * own later. Every other object answers with its current [CardComponent].
 */
fun ComponentContainer.copiableCardComponent(): CardComponent? =
    get<FlippedComponent>()?.unflippedCard ?: get<CardComponent>()
