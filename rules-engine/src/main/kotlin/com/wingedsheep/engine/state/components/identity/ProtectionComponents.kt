package com.wingedsheep.engine.state.components.identity

import com.wingedsheep.engine.state.Component
import com.wingedsheep.sdk.core.Color
import kotlinx.serialization.Serializable

/**
 * Static protection from one or more colors (Rule 702.16).
 * Attached to permanents/cards that have innate protection (e.g., Disciple of Grace).
 * Dynamic protection granted by spells (e.g., Akroma's Blessing) uses floating effects instead.
 */
@Serializable
data class ProtectionComponent(
    val colors: Set<Color>,
    val subtypes: Set<String> = emptySet()
) : Component
