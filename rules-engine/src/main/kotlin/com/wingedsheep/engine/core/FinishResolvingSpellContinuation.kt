package com.wingedsheep.engine.core

import com.wingedsheep.engine.state.ObjectRef
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.SpellOnStackComponent
import kotlinx.serialization.Serializable

/** Completes a spell only after all of its resolving effects and choices have finished. */
@Serializable
data class FinishResolvingSpellContinuation(
    override val decisionId: String,
    val spellObject: ObjectRef,
    val spellComponent: SpellOnStackComponent,
    val cardComponent: CardComponent?,
) : ContinuationFrame
