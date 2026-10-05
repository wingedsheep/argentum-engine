package com.wingedsheep.engine.state.components.identity

import com.wingedsheep.engine.state.Component
import kotlinx.serialization.Serializable

/**
 * The flipped status of a flip-card permanent (CR 110.5, 710).
 *
 * While present, the entity's [CardComponent] carries the flip half's name, type line, rules text
 * and P/T — with the card's own mana cost and colour kept (CR 710.1c). [unflippedCard] is the
 * current upright copiable identity, updated whenever the permanent recopies. It supplies the
 * upright half to later copies and lets
 * [com.wingedsheep.engine.handlers.effects.ZoneTransitionService] can restore it when the permanent
 * leaves the battlefield: a flipped permanent that leaves retains no memory of its status
 * (CR 710.4). Its presence is also what makes flipping one-way — a flipped permanent can't flip
 * again, and nothing un-flips it while it stays on the battlefield.
 */
@Serializable
data class FlippedComponent(
    val unflippedCard: CardComponent,
) : Component
