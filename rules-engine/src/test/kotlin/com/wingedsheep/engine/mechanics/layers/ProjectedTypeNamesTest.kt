package com.wingedsheep.engine.mechanics.layers

import com.wingedsheep.engine.core.CardEntityFactory
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.TextReplacement
import com.wingedsheep.engine.state.components.identity.TextReplacementCategory
import com.wingedsheep.engine.state.components.identity.TextReplacementComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ProjectedTypeNamesTest : FunSpec({
    test("projected type names preserve ordered supertypes, card types, and replaced subtypes") {
        val owner = EntityId.of("owner")
        val id = EntityId.of("permanent")
        val definition = card("Type Witness") {
            manaCost = "{0}"
            typeLine = "Legendary Artifact Creature — Bear Warrior"
            power = 2
            toughness = 2
        }
        val source = GameState(
            entities = mapOf(id to CardEntityFactory.create(definition, owner)),
            zones = mapOf(ZoneKey(owner, Zone.BATTLEFIELD) to listOf(id)),
        )
        val projector = StateProjector()
        val original = projector.project(source)
        original.getTypes(id).toList() shouldBe listOf("LEGENDARY", "ARTIFACT", "CREATURE", "Bear", "Warrior")
        original.getSubtypes(id).toList() shouldBe listOf("Bear", "Warrior")

        val changed = source.updateEntity(id) {
            it.with(TextReplacementComponent(listOf(TextReplacement("Bear", "Goblin", TextReplacementCategory.CREATURE_TYPE))))
        }
        val projected = projector.project(changed)
        projected.getTypes(id).toList() shouldBe listOf("LEGENDARY", "ARTIFACT", "CREATURE", "Goblin", "Warrior")
        projected.getSubtypes(id).toList() shouldBe listOf("Goblin", "Warrior")
        original.getTypes(id).toList() shouldBe listOf("LEGENDARY", "ARTIFACT", "CREATURE", "Bear", "Warrior")
    }
})
