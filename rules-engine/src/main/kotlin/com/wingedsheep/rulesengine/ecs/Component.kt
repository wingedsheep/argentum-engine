package com.wingedsheep.rulesengine.ecs

import kotlinx.serialization.Serializable

/**
 * Marker interface for all ECS components.
 *
 * Components are pure data bags with no behavior. They represent a single
 * aspect of an entity's state (e.g., "is tapped", "has power/toughness",
 * "is controlled by player X").
 *
 * Design principles:
 * - Components should be immutable (data classes or objects)
 * - Components should be serializable for game state persistence
 * - Components should represent a single concern
 * - Use data objects for boolean flags (presence = true, absence = false)
 *
 * Example usage:
 * ```kotlin
 * // Data class component with state
 * @Serializable
 * data class LifeComponent(val life: Int) : Component
 *
 * // Data object component for flags
 * @Serializable
 * data object TappedComponent : Component
 * ```
 */
interface Component
