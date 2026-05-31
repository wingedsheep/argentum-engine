package com.wingedsheep.engine.hygiene

import com.wingedsheep.engine.core.engineSerializersModule
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.serialization.json.Json

/**
 * Step 2 of `backlog/engine-performance.md`: [ComponentContainer] now keys its map by
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
})
