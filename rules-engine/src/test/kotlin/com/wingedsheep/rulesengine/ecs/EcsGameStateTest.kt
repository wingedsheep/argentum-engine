package com.wingedsheep.rulesengine.ecs

import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype
import com.wingedsheep.rulesengine.ecs.components.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

class EcsGameStateTest : FunSpec({

    val player1Id = EntityId.of("player1")
    val player2Id = EntityId.of("player2")

    fun newGame(): EcsGameState = EcsGameState.newGame(
        listOf(
            player1Id to "Alice",
            player2Id to "Bob"
        )
    )

    val bearDef = CardDefinition.creature(
        name = "Grizzly Bears",
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = setOf(Subtype.BEAST),
        power = 2,
        toughness = 2
    )

    context("newGame") {
        test("creates players with correct components") {
            val state = newGame()

            state.hasEntity(player1Id).shouldBeTrue()
            state.hasEntity(player2Id).shouldBeTrue()

            val player1 = state.getComponent<PlayerComponent>(player1Id)
            player1.shouldNotBeNull()
            player1.name shouldBe "Alice"

            val life = state.getComponent<LifeComponent>(player1Id)
            life.shouldNotBeNull()
            life.life shouldBe 20
        }

        test("creates empty zones") {
            val state = newGame()

            state.getZone(ZoneId.BATTLEFIELD).isEmpty().shouldBeTrue()
            state.getZone(ZoneId.STACK).isEmpty().shouldBeTrue()
            state.getZone(ZoneId.library(player1Id)).isEmpty().shouldBeTrue()
            state.getZone(ZoneId.hand(player1Id)).isEmpty().shouldBeTrue()
        }

        test("initializes turn state") {
            val state = newGame()

            state.turnNumber shouldBe 1
            state.activePlayerId shouldBe player1Id
        }
    }

    context("entity operations") {
        test("createEntity adds entity with components") {
            var state = newGame()
            val cardId = EntityId.generate()

            val (createdId, newState) = state.createEntity(
                cardId,
                CardComponent(bearDef, player1Id),
                ControllerComponent(player1Id)
            )

            createdId shouldBe cardId
            newState.hasEntity(cardId).shouldBeTrue()
            newState.getComponent<CardComponent>(cardId).shouldNotBeNull()
        }

        test("updateEntity modifies components") {
            var state = newGame()
            val (cardId, state2) = state.createEntity(
                EntityId.generate(),
                PTComponent(2, 2)
            )

            val updated = state2.updateEntity(cardId) { container ->
                container.with(PTComponent(3, 3))
            }

            updated.getComponent<PTComponent>(cardId)?.basePower shouldBe 3
        }

        test("addComponent adds to entity") {
            var state = newGame()
            val (cardId, state2) = state.createEntity(EntityId.generate())

            val updated = state2.addComponent(cardId, TappedComponent)

            updated.hasComponent<TappedComponent>(cardId).shouldBeTrue()
        }

        test("removeComponent removes from entity") {
            var state = newGame()
            val (cardId, state2) = state.createEntity(
                EntityId.generate(),
                TappedComponent
            )

            val updated = state2.removeComponent<TappedComponent>(cardId)

            updated.hasComponent<TappedComponent>(cardId).shouldBeFalse()
        }

        test("removeEntity removes from state and zones") {
            var state = newGame()
            val (cardId, state2) = state.createEntity(EntityId.generate())
            val state3 = state2.addToZone(cardId, ZoneId.BATTLEFIELD)

            val updated = state3.removeEntity(cardId)

            updated.hasEntity(cardId).shouldBeFalse()
            updated.getZone(ZoneId.BATTLEFIELD).contains(cardId).shouldBeFalse()
        }
    }

    context("zone operations") {
        test("addToZone adds entity to zone") {
            var state = newGame()
            val (cardId, state2) = state.createEntity(EntityId.generate())

            val updated = state2.addToZone(cardId, ZoneId.BATTLEFIELD)

            updated.getZone(ZoneId.BATTLEFIELD).contains(cardId).shouldBeTrue()
        }

        test("addToZoneBottom adds to beginning") {
            var state = newGame()
            val (id1, state2) = state.createEntity(EntityId.generate())
            val (id2, state3) = state2.createEntity(EntityId.generate())
            val state4 = state3.addToZone(id1, ZoneId.BATTLEFIELD)

            val updated = state4.addToZoneBottom(id2, ZoneId.BATTLEFIELD)

            updated.getZone(ZoneId.BATTLEFIELD).first() shouldBe id2
        }

        test("removeFromZone removes entity") {
            var state = newGame()
            val (cardId, state2) = state.createEntity(EntityId.generate())
            val state3 = state2.addToZone(cardId, ZoneId.BATTLEFIELD)

            val updated = state3.removeFromZone(cardId, ZoneId.BATTLEFIELD)

            updated.getZone(ZoneId.BATTLEFIELD).contains(cardId).shouldBeFalse()
        }

        test("moveEntity moves between zones") {
            var state = newGame()
            val (cardId, state2) = state.createEntity(EntityId.generate())
            val state3 = state2.addToZone(cardId, ZoneId.hand(player1Id))

            val updated = state3.moveEntity(cardId, ZoneId.hand(player1Id), ZoneId.BATTLEFIELD)

            updated.getZone(ZoneId.hand(player1Id)).contains(cardId).shouldBeFalse()
            updated.getZone(ZoneId.BATTLEFIELD).contains(cardId).shouldBeTrue()
        }

        test("findZone returns correct zone") {
            var state = newGame()
            val (cardId, state2) = state.createEntity(EntityId.generate())
            val state3 = state2.addToZone(cardId, ZoneId.BATTLEFIELD)

            state3.findZone(cardId) shouldBe ZoneId.BATTLEFIELD
        }

        test("findZone returns null for entity not in zone") {
            var state = newGame()
            val (cardId, state2) = state.createEntity(EntityId.generate())

            state2.findZone(cardId).shouldBeNull()
        }
    }

    context("queries") {
        test("entitiesWithComponent finds matching entities") {
            val state = newGame()

            val players = state.entitiesWithComponent<PlayerComponent>()

            players.size shouldBe 2
            players.contains(player1Id).shouldBeTrue()
            players.contains(player2Id).shouldBeTrue()
        }

        test("getPlayerIds returns player entities") {
            val state = newGame()

            val players = state.getPlayerIds()

            players.size shouldBe 2
        }

        test("getCreaturesOnBattlefield returns creatures") {
            var state = newGame()
            val (bearId, state2) = state.createEntity(
                EntityId.generate(),
                CardComponent(bearDef, player1Id),
                ControllerComponent(player1Id)
            )
            val state3 = state2.addToZone(bearId, ZoneId.BATTLEFIELD)

            val creatures = state3.getCreaturesOnBattlefield()

            creatures.size shouldBe 1
            creatures.contains(bearId).shouldBeTrue()
        }

        test("getPermanentsControlledBy returns matching permanents") {
            var state = newGame()
            val (bearId, state2) = state.createEntity(
                EntityId.generate(),
                CardComponent(bearDef, player1Id),
                ControllerComponent(player1Id)
            )
            val state3 = state2.addToZone(bearId, ZoneId.BATTLEFIELD)

            val permanents = state3.getPermanentsControlledBy(player1Id)

            permanents.size shouldBe 1
            permanents.contains(bearId).shouldBeTrue()

            state3.getPermanentsControlledBy(player2Id).isEmpty().shouldBeTrue()
        }
    }

    context("global flags") {
        test("setFlag and getFlag") {
            val state = newGame()
                .setFlag("test", "42")

            val value = state.getFlag("test")
            value shouldBe "42"
        }

        test("hasFlag") {
            val state = newGame()
                .setFlag("exists", "true")

            state.hasFlag("exists").shouldBeTrue()
            state.hasFlag("missing").shouldBeFalse()
        }

        test("removeFlag") {
            val state = newGame()
                .setFlag("temp", "123")
                .removeFlag("temp")

            state.hasFlag("temp").shouldBeFalse()
        }
    }

    context("stack operations") {
        test("stackIsEmpty for empty stack") {
            val state = newGame()

            state.stackIsEmpty.shouldBeTrue()
        }

        test("stackIsEmpty after adding") {
            var state = newGame()
            val (spellId, state2) = state.createEntity(EntityId.generate())
            val state3 = state2.addToZone(spellId, ZoneId.STACK)

            state3.stackIsEmpty.shouldBeFalse()
        }
    }
})
