package com.wingedsheep.engine.state.components.identity

import com.wingedsheep.engine.state.Component
import kotlinx.serialization.Serializable

/**
 * Tracks that an entity is a double-faced card (DFC) and which face is currently up.
 *
 * DFCs are represented as a single [com.wingedsheep.sdk.model.EntityId] whose [CardComponent]
 * always reflects the characteristics of the currently-up face. Transforming a DFC is a
 * wholesale swap of [CardComponent] (like Clone's copy effect) — the entity ID is stable,
 * counters/damage/attachments/controller persist, and only the identity characteristics
 * (name, type line, P/T, keywords, colors, oracle text, abilities) change.
 *
 * Rule 712.4: "A double-faced card that is outside the battlefield is considered to have
 * only the characteristics of its front face." If a DFC leaves the battlefield while on its
 * back face, it is reset to its front face.
 */
@Serializable
data class DoubleFacedComponent(
    /** Card definition id of the front face (the side that can be cast from hand). */
    val frontCardDefinitionId: String,
    /** Card definition id of the back face. */
    val backCardDefinitionId: String,
    /** Which face is currently up. */
    val currentFace: Face = Face.FRONT
) : Component {
    @Serializable
    enum class Face { FRONT, BACK }

    val isFront: Boolean get() = currentFace == Face.FRONT
    val isBack: Boolean get() = currentFace == Face.BACK
}
