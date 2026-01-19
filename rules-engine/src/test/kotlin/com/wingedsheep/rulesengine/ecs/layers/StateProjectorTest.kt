package com.wingedsheep.rulesengine.ecs.layers

import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.core.CounterType
import com.wingedsheep.rulesengine.core.Keyword
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype
import com.wingedsheep.rulesengine.ecs.GameState
import com.wingedsheep.rulesengine.ecs.EntityId
import com.wingedsheep.rulesengine.ecs.ZoneId
import com.wingedsheep.rulesengine.ecs.components.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

class StateProjectorTest : FunSpec({

    val player1Id = EntityId.of("player1")
    val player2Id = EntityId.of("player2")

    val bearDef = CardDefinition.creature(
        name = "Grizzly Bears",
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = setOf(Subtype.BEAST),
        power = 2,
        toughness = 2
    )

    val flyingBearDef = CardDefinition.creature(
        name = "Flying Bear",
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = setOf(Subtype.BEAST),
        power = 2,
        toughness = 2,
        keywords = setOf(Keyword.FLYING)
    )

    fun newGame(): GameState = GameState.newGame(
        listOf(player1Id to "Alice", player2Id to "Bob")
    )

    fun GameState.addBear(id: EntityId = EntityId.generate()): Pair<EntityId, GameState> {
        val (bearId, state1) = createEntity(
            id,
            CardComponent(bearDef, player1Id),
            ControllerComponent(player1Id)
        )
        return bearId to state1.addToZone(bearId, ZoneId.BATTLEFIELD)
    }

    beforeTest {
        Modifier.resetTimestamps()
    }

    context("base projection without modifiers") {
        test("projects creature with base stats") {
            val (bearId, state) = newGame().addBear()

            val projector = StateProjector(state)
            val view = projector.getView(bearId)

            view.shouldNotBeNull()
            view.name shouldBe "Grizzly Bears"
            view.power shouldBe 2
            view.toughness shouldBe 2
            view.isCreature.shouldBeTrue()
        }

        test("projects creature with base keywords") {
            var state = newGame()
            val (flyingBearId, state2) = state.createEntity(
                EntityId.generate(),
                CardComponent(flyingBearDef, player1Id),
                ControllerComponent(player1Id)
            )
            val state3 = state2.addToZone(flyingBearId, ZoneId.BATTLEFIELD)

            val projector = StateProjector(state3)
            val view = projector.getView(flyingBearId)

            view.shouldNotBeNull()
            view.hasKeyword(Keyword.FLYING).shouldBeTrue()
        }

        test("projects tapped state") {
            var state = newGame()
            val (bearId, state2) = state.createEntity(
                EntityId.generate(),
                CardComponent(bearDef, player1Id),
                ControllerComponent(player1Id),
                TappedComponent
            )
            val state3 = state2.addToZone(bearId, ZoneId.BATTLEFIELD)

            val projector = StateProjector(state3)
            val view = projector.getView(bearId)

            view.shouldNotBeNull()
            view.isTapped.shouldBeTrue()
        }

        test("projects damage") {
            var state = newGame()
            val (bearId, state2) = state.createEntity(
                EntityId.generate(),
                CardComponent(bearDef, player1Id),
                ControllerComponent(player1Id),
                DamageComponent(1)
            )
            val state3 = state2.addToZone(bearId, ZoneId.BATTLEFIELD)

            val projector = StateProjector(state3)
            val view = projector.getView(bearId)

            view.shouldNotBeNull()
            view.damage shouldBe 1
            view.effectiveToughness shouldBe 1
        }
    }

    context("P/T modifiers") {
        test("applies +1/+1 modifier") {
            val (bearId, state) = newGame().addBear()

            val modifier = Modifier(
                layer = Layer.PT_MODIFY,
                sourceId = EntityId.of("source"),
                timestamp = Modifier.nextTimestamp(),
                modification = Modification.ModifyPT(1, 1),
                filter = ModifierFilter.Specific(bearId)
            )

            val projector = StateProjector(state, listOf(modifier))
            val view = projector.getView(bearId)

            view.shouldNotBeNull()
            view.power shouldBe 3
            view.toughness shouldBe 3
        }

        test("applies multiple P/T modifiers in timestamp order") {
            val (bearId, state) = newGame().addBear()

            val modifier1 = Modifier(
                layer = Layer.PT_MODIFY,
                sourceId = EntityId.of("source1"),
                timestamp = Modifier.nextTimestamp(),
                modification = Modification.ModifyPT(2, 2),
                filter = ModifierFilter.Specific(bearId)
            )

            val modifier2 = Modifier(
                layer = Layer.PT_MODIFY,
                sourceId = EntityId.of("source2"),
                timestamp = Modifier.nextTimestamp(),
                modification = Modification.ModifyPT(1, 0),
                filter = ModifierFilter.Specific(bearId)
            )

            val projector = StateProjector(state, listOf(modifier1, modifier2))
            val view = projector.getView(bearId)

            view.shouldNotBeNull()
            view.power shouldBe 5  // 2 base + 2 + 1
            view.toughness shouldBe 4  // 2 base + 2 + 0
        }

        test("applies P/T set before P/T modify") {
            val (bearId, state) = newGame().addBear()

            // Set P/T to 1/1 (Layer 7b)
            val setModifier = Modifier(
                layer = Layer.PT_SET,
                sourceId = EntityId.of("humility"),
                timestamp = Modifier.nextTimestamp(),
                modification = Modification.SetPT(1, 1),
                filter = ModifierFilter.Specific(bearId)
            )

            // Then +2/+2 (Layer 7c) - should apply after set
            val modifyModifier = Modifier(
                layer = Layer.PT_MODIFY,
                sourceId = EntityId.of("giant_growth"),
                timestamp = Modifier.nextTimestamp(),
                modification = Modification.ModifyPT(2, 2),
                filter = ModifierFilter.Specific(bearId)
            )

            val projector = StateProjector(state, listOf(modifyModifier, setModifier))
            val view = projector.getView(bearId)

            view.shouldNotBeNull()
            view.power shouldBe 3  // Set to 1, then +2
            view.toughness shouldBe 3  // Set to 1, then +2
        }
    }

    context("counters (Layer 7d)") {
        test("applies +1/+1 counters") {
            var state = newGame()
            val (bearId, state2) = state.createEntity(
                EntityId.generate(),
                CardComponent(bearDef, player1Id),
                ControllerComponent(player1Id),
                CountersComponent().add(CounterType.PLUS_ONE_PLUS_ONE, 2)
            )
            val state3 = state2.addToZone(bearId, ZoneId.BATTLEFIELD)

            val projector = StateProjector(state3)
            val view = projector.getView(bearId)

            view.shouldNotBeNull()
            view.power shouldBe 4  // 2 base + 2 counters
            view.toughness shouldBe 4
        }

        test("applies -1/-1 counters") {
            var state = newGame()
            val (bearId, state2) = state.createEntity(
                EntityId.generate(),
                CardComponent(bearDef, player1Id),
                ControllerComponent(player1Id),
                CountersComponent().add(CounterType.MINUS_ONE_MINUS_ONE, 1)
            )
            val state3 = state2.addToZone(bearId, ZoneId.BATTLEFIELD)

            val projector = StateProjector(state3)
            val view = projector.getView(bearId)

            view.shouldNotBeNull()
            view.power shouldBe 1  // 2 base - 1 counter
            view.toughness shouldBe 1
        }

        test("counters applied after P/T modifications") {
            var state = newGame()
            val (bearId, state2) = state.createEntity(
                EntityId.generate(),
                CardComponent(bearDef, player1Id),
                ControllerComponent(player1Id),
                CountersComponent().add(CounterType.PLUS_ONE_PLUS_ONE, 1)
            )
            val state3 = state2.addToZone(bearId, ZoneId.BATTLEFIELD)

            // +1/+1 from modifier
            val modifier = Modifier(
                layer = Layer.PT_MODIFY,
                sourceId = EntityId.of("anthem"),
                timestamp = Modifier.nextTimestamp(),
                modification = Modification.ModifyPT(1, 1),
                filter = ModifierFilter.Specific(bearId)
            )

            val projector = StateProjector(state3, listOf(modifier))
            val view = projector.getView(bearId)

            view.shouldNotBeNull()
            view.power shouldBe 4  // 2 base + 1 modifier + 1 counter
            view.toughness shouldBe 4
        }
    }

    context("keyword modifiers") {
        test("adds keyword to creature") {
            val (bearId, state) = newGame().addBear()

            val modifier = Modifier(
                layer = Layer.ABILITY,
                sourceId = EntityId.of("levitation"),
                timestamp = Modifier.nextTimestamp(),
                modification = Modification.AddKeyword(Keyword.FLYING),
                filter = ModifierFilter.Specific(bearId)
            )

            val projector = StateProjector(state, listOf(modifier))
            val view = projector.getView(bearId)

            view.shouldNotBeNull()
            view.hasKeyword(Keyword.FLYING).shouldBeTrue()
        }

        test("removes keyword from creature") {
            var state = newGame()
            val (flyingBearId, state2) = state.createEntity(
                EntityId.generate(),
                CardComponent(flyingBearDef, player1Id),
                ControllerComponent(player1Id)
            )
            val state3 = state2.addToZone(flyingBearId, ZoneId.BATTLEFIELD)

            val modifier = Modifier(
                layer = Layer.ABILITY,
                sourceId = EntityId.of("gravity_well"),
                timestamp = Modifier.nextTimestamp(),
                modification = Modification.RemoveKeyword(Keyword.FLYING),
                filter = ModifierFilter.Specific(flyingBearId)
            )

            val projector = StateProjector(state3, listOf(modifier))
            val view = projector.getView(flyingBearId)

            view.shouldNotBeNull()
            view.hasKeyword(Keyword.FLYING).shouldBeFalse()
        }
    }

    context("control modifiers") {
        test("changes controller of permanent") {
            val (bearId, state) = newGame().addBear()

            val modifier = Modifier(
                layer = Layer.CONTROL,
                sourceId = EntityId.of("control_magic"),
                timestamp = Modifier.nextTimestamp(),
                modification = Modification.ChangeControl(player2Id),
                filter = ModifierFilter.Specific(bearId)
            )

            val projector = StateProjector(state, listOf(modifier))
            val view = projector.getView(bearId)

            view.shouldNotBeNull()
            view.controllerId shouldBe player2Id
            view.ownerId shouldBe player1Id  // Owner doesn't change
        }
    }

    context("modifier filters") {
        test("All filter affects matching creatures") {
            var state = newGame()
            val (bear1Id, state2) = state.addBear(EntityId.of("bear1"))
            val (bear2Id, state3) = state2.addBear(EntityId.of("bear2"))

            val modifier = Modifier(
                layer = Layer.PT_MODIFY,
                sourceId = EntityId.of("anthem"),
                timestamp = Modifier.nextTimestamp(),
                modification = Modification.ModifyPT(1, 1),
                filter = ModifierFilter.All(EntityCriteria.Creatures)
            )

            val projector = StateProjector(state3, listOf(modifier))

            val view1 = projector.getView(bear1Id)
            val view2 = projector.getView(bear2Id)

            view1.shouldNotBeNull()
            view1.power shouldBe 3
            view2.shouldNotBeNull()
            view2.power shouldBe 3
        }

        test("ControlledBy filter only affects that player's creatures") {
            var state = newGame()
            // Player 1's bear
            val (bear1Id, state2) = state.createEntity(
                EntityId.of("bear1"),
                CardComponent(bearDef, player1Id),
                ControllerComponent(player1Id)
            )
            val state3 = state2.addToZone(bear1Id, ZoneId.BATTLEFIELD)

            // Player 2's bear
            val (bear2Id, state4) = state3.createEntity(
                EntityId.of("bear2"),
                CardComponent(bearDef, player2Id),
                ControllerComponent(player2Id)
            )
            val state5 = state4.addToZone(bear2Id, ZoneId.BATTLEFIELD)

            val modifier = Modifier(
                layer = Layer.PT_MODIFY,
                sourceId = EntityId.of("anthem"),
                timestamp = Modifier.nextTimestamp(),
                modification = Modification.ModifyPT(1, 1),
                filter = ModifierFilter.ControlledBy(player1Id)
            )

            val projector = StateProjector(state5, listOf(modifier))

            val view1 = projector.getView(bear1Id)
            val view2 = projector.getView(bear2Id)

            view1.shouldNotBeNull()
            view1.power shouldBe 3  // Affected
            view2.shouldNotBeNull()
            view2.power shouldBe 2  // Not affected
        }
    }

    context("battlefield projection") {
        test("projectBattlefield returns all permanents") {
            var state = newGame()
            val (bear1Id, state2) = state.addBear(EntityId.of("bear1"))
            val (bear2Id, state3) = state2.addBear(EntityId.of("bear2"))

            val projector = StateProjector(state3)
            val views = projector.projectBattlefield()

            views.size shouldBe 2
        }

        test("projectCreatures returns only creatures") {
            var state = newGame()
            val (bearId, state2) = state.addBear()

            // Add a non-creature (forest)
            val forestDef = CardDefinition.basicLand("Forest", Subtype.FOREST)
            val (forestId, state3) = state2.createEntity(
                EntityId.generate(),
                CardComponent(forestDef, player1Id),
                ControllerComponent(player1Id)
            )
            val state4 = state3.addToZone(forestId, ZoneId.BATTLEFIELD)

            val projector = StateProjector(state4)
            val creatures = projector.projectCreatures()

            creatures.size shouldBe 1
            creatures[0].entityId shouldBe bearId
        }
    }

    context("Ability/Restriction modifiers") {
        test("AddCantBlockRestriction prevents blocking") {
            val (bearId, state) = newGame().addBear()

            val modifier = Modifier(
                layer = Layer.ABILITY,
                sourceId = EntityId.of("source"),
                timestamp = Modifier.nextTimestamp(),
                modification = Modification.AddCantBlockRestriction,
                filter = ModifierFilter.Specific(bearId)
            )

            val projector = StateProjector(state, listOf(modifier))
            val view = projector.getView(bearId)

            view.shouldNotBeNull()
            // The flag should be set
            view.cantBlock.shouldBeTrue()
            // The derived property should prevent blocking
            view.canBlock.shouldBeFalse()
        }
    }
})
