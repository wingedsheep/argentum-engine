package com.wingedsheep.engine.state.components.identity

import com.wingedsheep.engine.state.Component
import kotlinx.serialization.Serializable

/**
 * Marks an entity as the synthetic source of a static-ability emblem.
 *
 * Static-ability emblems (e.g., Oko, Shadowmoor Scion's -6) don't live in any zone — the
 * executor creates a hidden entity carrying the controller and any chosen creature type,
 * then attaches floating effects. This component lets the client transformer locate those
 * synthetic sources and render an emblem badge on the controller's player effects panel.
 *
 * @property sourceName The original creating card's name (e.g., "Oko, Shadowmoor Scion") for badge display
 * @property description Resolved emblem text shown in the badge tooltip
 */
@Serializable
data class EmblemSourceComponent(
    val sourceName: String,
    val description: String
) : Component
