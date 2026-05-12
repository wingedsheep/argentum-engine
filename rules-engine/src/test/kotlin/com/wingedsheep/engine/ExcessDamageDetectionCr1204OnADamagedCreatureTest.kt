package com.wingedsheep.engine

import com.wingedsheep.engine.core.DamageDealtEvent
import com.wingedsheep.engine.handlers.effects.DamageUtils
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Format
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CreatureStats
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * CR 120.4: excess damage dealt to a creature = damage dealt minus remaining toughness.
 * The engine's damage event carries this value pre-computed so subscribers can act on it
 * without requerying creature state.
 */
class ExcessDamageDetectionCr1204OnADamagedCreatureTest : FunSpec({

    beforeTest {
        DamageUtils.cardRegistry = CardRegistry()
    }

    test("damage event reports excess above remaining toughness per CR 120.4") {
        // GIVEN a creature with toughness 2 and no prior damage marked on it
        val ownerId = EntityId.generate()
        val creatureId = EntityId.generate()

        val creatureContainer = ComponentContainer.of(
            CardComponent(
                cardDefinitionId = "test/3-2-rhino",
                name = "Test Rhino",
                manaCost = ManaCost.parse("{2}{G}"),
                typeLine = TypeLine.parse("Creature — Rhino"),
                baseStats = CreatureStats(3, 2)
            )
        )

        val state = GameState(format = Format.Standard)
            .withEntity(ownerId, ComponentContainer.of())
            .withEntity(creatureId, creatureContainer)
            .addToZone(ZoneKey(ownerId, Zone.BATTLEFIELD), creatureId)

        // AND a source that deals 5 damage to that creature in a single assignment
        val result = DamageUtils.dealDamageToTarget(state, creatureId, 5, null)

        // WHEN the engine resolves the damage and emits the resulting damage event
        val event = result.events.filterIsInstance<DamageDealtEvent>().single()

        // THEN the emitted event reports amount = 5
        event.amount shouldBe 5

        // AND excessDamage = 3 (5 damage − 2 remaining toughness per CR 120.4)
        // A subscriber can read this without recomputing from creature state.
        event.excessDamage shouldBe 3
    }
})
