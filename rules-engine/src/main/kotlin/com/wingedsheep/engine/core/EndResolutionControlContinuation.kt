package com.wingedsheep.engine.core

import com.wingedsheep.engine.state.ObjectRef
import kotlinx.serialization.Serializable

/** Bottom frame of a stack object's resolution; retains its identity after it leaves the stack. */
@Serializable
data class EndResolutionControlContinuation(val resolvingObject: ObjectRef) : AutomaticContinuation
