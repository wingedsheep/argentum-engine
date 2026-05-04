package com.wingedsheep.engine.state.components.identity

import com.wingedsheep.engine.state.Component
import kotlinx.serialization.Serializable

/**
 * Printed Toxic N (Rule 702.164). Seeded at game initialization from
 * `KeywordAbility.Toxic` on the card definition. The state projector emits
 * `TOXIC_<amount>` into projected keywords so combat damage can read the
 * total toxic value (printed + granted) from a single source of truth.
 */
@Serializable
data class ToxicComponent(val amount: Int) : Component
