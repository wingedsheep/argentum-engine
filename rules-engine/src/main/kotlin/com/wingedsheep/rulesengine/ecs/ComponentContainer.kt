package com.wingedsheep.rulesengine.ecs

import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

/**
 * Container for all components attached to a single entity.
 *
 * Provides type-safe component access using Kotlin's reified generics.
 * Components are stored by their class name for serialization compatibility.
 *
 * Example usage:
 * ```kotlin
 * val container = ComponentContainer.of(
 *     LifeComponent(20),
 *     TappedComponent
 * )
 *
 * // Type-safe access
 * val life: LifeComponent? = container.get<LifeComponent>()
 * val isTapped: Boolean = container.has<TappedComponent>()
 *
 * // Immutable updates
 * val newContainer = container.with(LifeComponent(19))
 * val untapped = container.without<TappedComponent>()
 * ```
 */
@Serializable
data class ComponentContainer(
    @PublishedApi
    internal val components: Map<String, Component> = emptyMap()
) {
    /**
     * Get a component by type.
     * Returns null if the component is not present.
     */
    inline fun <reified T : Component> get(): T? =
        components[T::class.simpleName] as? T

    /**
     * Get a component by KClass.
     * Returns null if the component is not present.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Component> get(type: KClass<T>): T? =
        components[type.simpleName] as? T

    /**
     * Check if this container has a specific component type.
     */
    inline fun <reified T : Component> has(): Boolean =
        components.containsKey(T::class.simpleName)

    /**
     * Check if this container has a specific component type by KClass.
     */
    fun <T : Component> has(type: KClass<T>): Boolean =
        components.containsKey(type.simpleName)

    /**
     * Add or update a component.
     * Returns a new container with the component added/updated.
     */
    fun <T : Component> with(component: T): ComponentContainer =
        copy(components = components + (component::class.simpleName!! to component))

    /**
     * Remove a component by type.
     * Returns a new container without the component.
     */
    inline fun <reified T : Component> without(): ComponentContainer =
        copy(components = components - T::class.simpleName!!)

    /**
     * Remove a component by KClass.
     * Returns a new container without the component.
     */
    fun <T : Component> without(type: KClass<T>): ComponentContainer =
        copy(components = components - type.simpleName!!)

    /**
     * Get all components in this container.
     */
    fun all(): Collection<Component> = components.values

    /**
     * Get all component type names in this container.
     */
    fun componentTypes(): Set<String> = components.keys

    /**
     * Check if this container is empty.
     */
    fun isEmpty(): Boolean = components.isEmpty()

    /**
     * Check if this container is not empty.
     */
    fun isNotEmpty(): Boolean = components.isNotEmpty()

    /**
     * Get the number of components in this container.
     */
    val size: Int get() = components.size

    companion object {
        /**
         * Create an empty component container.
         */
        fun empty(): ComponentContainer = ComponentContainer()

        /**
         * Create a component container with the given components.
         */
        fun of(vararg components: Component): ComponentContainer =
            ComponentContainer(components.associateBy { it::class.simpleName!! })

        /**
         * Create a component container from a list of components.
         */
        fun of(components: List<Component>): ComponentContainer =
            ComponentContainer(components.associateBy { it::class.simpleName!! })
    }
}
