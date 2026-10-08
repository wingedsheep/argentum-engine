package com.wingedsheep.engine.hygiene

import com.wingedsheep.engine.core.engineSerializersModule
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.Component
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.serialization.json.Json

/**
 * Step 2 of `backlog/archived/engine-performance-hotspots.md`: [ComponentContainer] now keys its map by
 * [Class] for identity-hash lookups, with a custom [com.wingedsheep.engine.state.ComponentContainerSerializer].
 * These tests pin the serializer's behaviour — that a live container survives a JSON
 * round-trip (the components, and their type-keyed accessibility, are preserved) and
 * that the on-disk form still uses class-name string keys.
 */
class ComponentContainerSerializationRoundTripTest : FunSpec({

    val json = Json {
        serializersModule = engineSerializersModule
        encodeDefaults = true
    }

    test("container round-trips: components recovered and accessible by type") {
        val original = ComponentContainer()
            .with(TappedComponent)
            .with(SummoningSicknessComponent)
            .with(LifeTotalComponent(37))

        val encoded = json.encodeToString(ComponentContainer.serializer(), original)
        val decoded = json.decodeFromString(ComponentContainer.serializer(), encoded)

        decoded shouldBe original
        decoded.has<TappedComponent>() shouldBe true
        decoded.has<SummoningSicknessComponent>() shouldBe true
        decoded.get<LifeTotalComponent>() shouldBe LifeTotalComponent(37)
    }

    test("wire format keys the map by fully-qualified class name") {
        val encoded = json.encodeToString(
            ComponentContainer.serializer(),
            ComponentContainer().with(LifeTotalComponent(20))
        )
        encoded shouldContain LifeTotalComponent::class.java.name
    }

    test("empty container round-trips") {
        val decoded = json.decodeFromString(
            ComponentContainer.serializer(),
            json.encodeToString(ComponentContainer.serializer(), ComponentContainer.EMPTY)
        )
        decoded.isEmpty() shouldBe true
    }

    test("factory retains empty identity and keys a single component by runtime type") {
        (ComponentContainer.of() === ComponentContainer.EMPTY) shouldBe true
        val component: Component = LifeTotalComponent(20)
        val container = ComponentContainer.of(component)
        container.get<LifeTotalComponent>() shouldBe component
        container.get<Component>() shouldBe null
    }

    test("factory replaces duplicate classes without moving their iteration or serialized position") {
        val components: Array<Component> = arrayOf(LifeTotalComponent(20), TappedComponent, LifeTotalComponent(37))
        val container = ComponentContainer.of(*components)
        val expected = listOf(LifeTotalComponent(37), TappedComponent)

        container.all().toList() shouldBe expected
        container.get<LifeTotalComponent>() shouldBe LifeTotalComponent(37)
        container.has<TappedComponent>() shouldBe true
        val encoded = json.encodeToString(ComponentContainer.serializer(), container)
        val sequential = ComponentContainer.EMPTY.withComponent(LifeTotalComponent(37)).withComponent(TappedComponent)
        encoded shouldBe json.encodeToString(ComponentContainer.serializer(), sequential)
        json.decodeFromString(ComponentContainer.serializer(), encoded).all().toList() shouldBe expected

        components[0] = SummoningSicknessComponent
        container.with(LifeTotalComponent(1)).without<TappedComponent>()
        ComponentContainer.of(SummoningSicknessComponent, LifeTotalComponent(2))
        container.all().toList() shouldBe expected
    }

    test("removing an absent exact component type retains the container") {
        val container = ComponentContainer.of(LifeTotalComponent(20), SummoningSicknessComponent)
        val encoded = json.encodeToString(ComponentContainer.serializer(), container)

        (container.without<TappedComponent>() === container) shouldBe true
        (container.without<Component>() === container) shouldBe true
        (ComponentContainer.EMPTY.without<TappedComponent>() === ComponentContainer.EMPTY) shouldBe true
        json.encodeToString(ComponentContainer.serializer(), container) shouldBe encoded
    }

    test("removing a present component preserves the source and remaining component order") {
        val container = ComponentContainer.of(LifeTotalComponent(20), TappedComponent, SummoningSicknessComponent)
        val removed = container.without<TappedComponent>()

        removed.has<TappedComponent>() shouldBe false
        removed.all().toList() shouldBe listOf(LifeTotalComponent(20), SummoningSicknessComponent)
        container.all().toList() shouldBe listOf(LifeTotalComponent(20), TappedComponent, SummoningSicknessComponent)
        json.decodeFromString(ComponentContainer.serializer(), json.encodeToString(ComponentContainer.serializer(), removed)) shouldBe removed
    }
})
