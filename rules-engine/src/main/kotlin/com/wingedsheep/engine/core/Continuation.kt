package com.wingedsheep.engine.core

import kotlinx.serialization.Serializable

/**
 * Represents a continuation frame - a reified "what to do next" after a decision.
 *
 * When the engine pauses for player input (e.g., "choose cards to discard"),
 * it pushes a ContinuationFrame onto the stack describing how to resume
 * execution once the player responds.
 *
 * This is a serializable alternative to closures/lambdas, allowing the
 * continuation state to be persisted and transferred across sessions.
 */
@Serializable
sealed interface ContinuationFrame {
    /** The decision ID this continuation is waiting for */
    val decisionId: String
}
