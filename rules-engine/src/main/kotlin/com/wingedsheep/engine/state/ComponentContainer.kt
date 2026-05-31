package com.wingedsheep.engine.state

import kotlinx.serialization.KSerializer
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Immutable container for an entity's components.
 * Components are accessed by their type.
 *
 * Components are keyed in-memory by their [Class] (identity hashCode — no string
 * hashing on the hot path). For serialization, [ComponentContainerSerializer] writes
 * the map with class-name string keys so the on-disk form stays human-readable and
 * stable across runs.
 */
@Serializable(with = ComponentContainerSerializer::class)
data class ComponentContainer(
    @PublishedApi internal val components: Map<Class<*>, Component> = emptyMap()
) {
    /**
     * Get a component by type, or null if not present.
     */
    inline fun <reified T : Component> get(): T? {
        return components[T::class.java] as? T
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
        return components.containsKey(T::class.java)
    }

    /**
     * Add or replace a component (returns new container).
     */
    inline fun <reified T : Component> with(component: T): ComponentContainer {
        return ComponentContainer(components + (T::class.java to component))
    }

    /**
     * Remove a component type (returns new container).
     */
    inline fun <reified T : Component> without(): ComponentContainer {
        return ComponentContainer(components - T::class.java)
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
        val key = component::class.java
        return ComponentContainer(components + (key to component))
    }
}

/**
 * Serializes [ComponentContainer] as a plain map of class-name → component
 * (`{"<class-name>": <component>, ...}`). The in-memory map is keyed by [Class] for
 * identity-hash lookups on the hot path; the serialized form uses the class name so it
 * stays readable and independent of JVM object identity.
 *
 * On deserialize each key is recovered from the polymorphically-decoded component's own
 * runtime class — no `Class.forName` lookup, and robust to whatever name was stored.
 */
object ComponentContainerSerializer : KSerializer<ComponentContainer> {
    private val delegate = MapSerializer(String.serializer(), PolymorphicSerializer(Component::class))

    override val descriptor: SerialDescriptor = delegate.descriptor

    override fun serialize(encoder: Encoder, value: ComponentContainer) {
        val byName = value.components.entries.associate { (cls, component) -> cls.name to component }
        encoder.encodeSerializableValue(delegate, byName)
    }

    override fun deserialize(decoder: Decoder): ComponentContainer {
        val byName = decoder.decodeSerializableValue(delegate)
        return ComponentContainer(byName.values.associateBy { it::class.java })
    }
}
