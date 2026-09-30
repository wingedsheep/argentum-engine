package com.wingedsheep.engine.state

import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.TextChangedEvent
import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.core.engineSerializersModule
import com.wingedsheep.engine.state.components.identity.TextReplacement
import com.wingedsheep.engine.state.components.identity.TextReplacementCategory
import com.wingedsheep.engine.state.components.identity.TextReplacementComponent
import com.wingedsheep.engine.state.components.stack.EntitySnapshot
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class TextChangeSnapshotSerializationTest : FunSpec({
    val json = Json { serializersModule = engineSerializersModule; encodeDefaults = true }
    val id = EntityId("changed-card")
    test("saved zone events retain effective text for a departed source") {
        val text = TextReplacementComponent(listOf(TextReplacement("Red", "Blue", TextReplacementCategory.COLOR_WORD)))
        val event: GameEvent = ZoneChangeEvent(id, "Word Witness", Zone.BATTLEFIELD, Zone.GRAVEYARD, EntityId("owner"),
            lastKnown = EntitySnapshot(id, textChanges = text))
        val restored = json.decodeFromString<GameEvent>(json.encodeToString(event)) as ZoneChangeEvent
        restored.lastKnown!!.textChanges!!.replaceColor(Color.RED) shouldBe Color.BLUE
        restored shouldBe event
    }
    test("applied text-change notifications round trip through the engine event protocol") {
        val event: GameEvent = TextChangedEvent(id, "Forest", "Island")
        json.decodeFromString<GameEvent>(json.encodeToString(event)) shouldBe event
    }
})
