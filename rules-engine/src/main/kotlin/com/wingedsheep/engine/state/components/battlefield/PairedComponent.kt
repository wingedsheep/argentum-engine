package com.wingedsheep.engine.state.components.battlefield

import com.wingedsheep.engine.state.Component
import com.wingedsheep.sdk.model.EntityId
import kotlinx.serialization.Serializable

/**
 * This creature is soulbond-**paired** with [partnerId] (CR 702.95b). Stamped on *both* halves by
 * `PairWithSourceExecutor`, always symmetrically — every invariant downstream (the
 * `Scope.SoulbondPair` affected set, the `IsPaired` predicate, the CR 702.95e break check) assumes
 * that if A points at B then B points back at A.
 *
 * A creature can be paired with only one other creature (CR 702.95d), which is why this holds a
 * single id rather than a set: a second pairing attempt on an already-paired creature is refused at
 * the effect, not silently overwritten here.
 *
 * The pair is a piece of state with its own lifetime, not a duration-bounded continuous effect —
 * `SoulbondPairingCheck` removes the component from both halves as soon as either stops being a
 * creature on the battlefield under the same controller.
 */
@Serializable
data class PairedComponent(
    val partnerId: EntityId
) : Component
