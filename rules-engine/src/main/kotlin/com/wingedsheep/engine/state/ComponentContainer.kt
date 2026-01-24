package com.wingedsheep.engine.state

import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

/**
 * Immutable container for an entity's components.
 * Components are accessed by their type.
 */
@Serializable
data class ComponentContainer(
    @PublishedApi internal val components: Map<String, Component> = emptyMap()
) {
    /**
     * Get a component by type, or null if not present.
     */
    inline fun <reified T : Component> get(): T? {
        return components[T::class.qualifiedName] as? T
    }

    /**
     * Get a component by type, throwing if not present.
     */
    inline fun <reified T : Component> require(): T {
        return get<T>() ?: throw IllegalStateException(
            "Required component ${T::class.simpleName} not found"
        )
    }

    /**
     * Check if a component type is present.
     */
    inline fun <reified T : Component> has(): Boolean {
        return components.containsKey(T::class.qualifiedName)
    }

    /**
     * Add or replace a component (returns new container).
     */
    inline fun <reified T : Component> with(component: T): ComponentContainer {
        return ComponentContainer(components + (T::class.qualifiedName!! to component))
    }

    /**
     * Remove a component type (returns new container).
     */
    inline fun <reified T : Component> without(): ComponentContainer {
        return ComponentContainer(components - T::class.qualifiedName!!)
    }

    /**
     * Get all components.
     */
    fun all(): Collection<Component> = components.values

    /**
     * Check if container is empty.
     */
    fun isEmpty(): Boolean = components.isEmpty()

    companion object {
        val EMPTY = ComponentContainer()

        /**
         * Create a container with the given components.
         */
        fun of(vararg components: Component): ComponentContainer {
            return components.fold(EMPTY) { container, component ->
                container.withComponent(component)
            }
        }
    }

    /**
     * Internal method to add a component without inline reification.
     * Used by the of() factory method.
     */
    fun withComponent(component: Component): ComponentContainer {
        val key = component::class.qualifiedName!!
        return ComponentContainer(components + (key to component))
    }
}
