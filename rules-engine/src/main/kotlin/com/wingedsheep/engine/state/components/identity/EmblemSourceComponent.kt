package com.wingedsheep.engine.state.components.identity

import com.wingedsheep.engine.state.Component
import com.wingedsheep.sdk.model.EntityId
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
 * @property creatorId The object whose ability created the emblem, and [creatorEntryTimestamp] the
 *   battlefield visit it was on. An emblem's text that names its creator — "cards exiled with Tibalt,
 *   Cosmic Impostor" — means that specific object (CR 400.7), so the emblem reads that visit's
 *   linked-exile pile, live or departed (see `LinkedExileLookup.exiledWith`).
 */
@Serializable
data class EmblemSourceComponent(
    val sourceName: String,
    val description: String,
    val creatorId: EntityId? = null,
    val creatorEntryTimestamp: Long? = null,
) : Component
