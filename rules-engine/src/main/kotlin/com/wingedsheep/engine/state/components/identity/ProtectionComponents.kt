package com.wingedsheep.engine.state.components.identity

import com.wingedsheep.engine.state.Component
import com.wingedsheep.sdk.core.Color
import kotlinx.serialization.Serializable

/**
 * Static protection from one or more qualities (Rule 702.16) printed on the card itself.
 * Attached to permanents/cards that have innate protection (e.g., Disciple of Grace).
 * Dynamic protection granted by spells (e.g., Akroma's Blessing) uses floating effects instead.
 *
 * Each quality is projected as its own keyword — `PROTECTION_FROM_<COLOR>`,
 * `PROTECTION_FROM_SUBTYPE_<X>`, `PROTECTION_FROM_SUPERTYPE_<X>`,
 * `PROTECTION_FROM_CARDTYPE_<TYPE>` — which is what the targeting, damage-prevention, and
 * block-evasion checks consult. A quality that lands in no field here is silently unenforced,
 * so every [com.wingedsheep.sdk.scripting.ProtectionScope] the engine honors needs a home.
 *
 * @property cardTypes Uppercase card type names, e.g. `INSTANT` for "protection from instants".
 */
@Serializable
data class ProtectionComponent(
    val colors: Set<Color>,
    val subtypes: Set<String> = emptySet(),
    val supertypes: Set<String> = emptySet(),
    val cardTypes: Set<String> = emptySet()
) : Component
