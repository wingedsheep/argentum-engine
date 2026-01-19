package com.wingedsheep.rulesengine.ecs.action

import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.core.Color
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype
import com.wingedsheep.rulesengine.ecs.GameState
import com.wingedsheep.rulesengine.ecs.EntityId
import com.wingedsheep.rulesengine.ecs.ZoneId
import com.wingedsheep.rulesengine.ecs.components.*
import com.wingedsheep.rulesengine.zone.ZoneType
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class GameActionHandlerTest : FunSpec({

    val player1Id = EntityId.of("player1")
    val player2Id = EntityId.of("player2")

    val bearDef = CardDefinition.creature(
        name = "Grizzly Bears",
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = setOf(Subtype.BEAST),
        power = 2,
        toughness = 2
    )

    val forestDef = CardDefinition.basicLand("Forest", Subtype.FOREST)

    fun newGame(): GameState = GameState.newGame(
        listOf(player1Id to "Alice", player2Id to "Bob")
    )

    fun GameState.addBearToHand(playerId: EntityId): Pair<EntityId, GameState> {
        val (bearId, state1) = createEntity(
            EntityId.generate(),
            CardComponent(bearDef, playerId),
            ControllerComponent(playerId)
        )
        return bearId to state1.addToZone(bearId, ZoneId.hand(playerId))
    }

    fun GameState.addBearToBattlefield(controllerId: EntityId): Pair<EntityId, GameState> {
        val (bearId, state1) = createEntity(
            EntityId.generate(),
            CardComponent(bearDef, controllerId),
            ControllerComponent(controllerId)
        )
        return bearId to state1.addToZone(bearId, ZoneId.BATTLEFIELD)
    }

    fun GameState.addForestToHand(playerId: EntityId): Pair<EntityId, GameState> {
        val (forestId, state1) = createEntity(
            EntityId.generate(),
            CardComponent(forestDef, playerId),
            ControllerComponent(playerId)
        )
        return forestId to state1.addToZone(forestId, ZoneId.hand(playerId))
    }

    val handler = GameActionHandler()

    context("life actions") {
        test("gain life increases life total") {
            val state = newGame()
            val action = GainLife(player1Id, 5)

            val result = handler.execute(state, action)

            result.shouldBeInstanceOf<GameActionResult.Success>()
            val success = result as GameActionResult.Success
            success.state.getEntity(player1Id)?.get<LifeComponent>()?.life shouldBe 25

            success.events shouldHaveSize 1
            success.events[0].shouldBeInstanceOf<GameActionEvent.LifeChanged>()
        }

        test("lose life decreases life total") {
            val state = newGame()
            val action = LoseLife(player1Id, 3)

            val result = handler.execute(state, action)

            result.shouldBeInstanceOf<GameActionResult.Success>()
            val success = result as GameActionResult.Success
            success.state.getEntity(player1Id)?.get<LifeComponent>()?.life shouldBe 17
        }

        test("deal damage to player decreases life") {
            val state = newGame()
            val action = DealDamageToPlayer(player2Id, 4, player1Id)

            val result = handler.execute(state, action)

            result.shouldBeInstanceOf<GameActionResult.Success>()
            val success = result as GameActionResult.Success
            success.state.getEntity(player2Id)?.get<LifeComponent>()?.life shouldBe 16

            success.events shouldHaveSize 2
            success.events[0].shouldBeInstanceOf<GameActionEvent.DamageDealtToPlayer>()
            success.events[1].shouldBeInstanceOf<GameActionEvent.LifeChanged>()
        }
    }

    context("mana actions") {
        test("add colored mana") {
            val state = newGame()
            val action = AddMana(player1Id, Color.GREEN, 2)

            val result = handler.execute(state, action)

            result.shouldBeInstanceOf<GameActionResult.Success>()
            val success = result as GameActionResult.Success
            success.state.getEntity(player1Id)?.get<ManaPoolComponent>()?.pool?.green shouldBe 2
        }

        test("add colorless mana") {
            val state = newGame()
            val action = AddColorlessMana(player1Id, 3)

            val result = handler.execute(state, action)

            result.shouldBeInstanceOf<GameActionResult.Success>()
            val success = result as GameActionResult.Success
            success.state.getEntity(player1Id)?.get<ManaPoolComponent>()?.pool?.colorless shouldBe 3
        }

        test("empty mana pool") {
            // First add mana, then empty
            var state = newGame()
            state = (handler.execute(state, AddMana(player1Id, Color.GREEN, 2)) as GameActionResult.Success).state

            val result = handler.execute(state, EmptyManaPool(player1Id))

            result.shouldBeInstanceOf<GameActionResult.Success>()
            val success = result as GameActionResult.Success
            success.state.getEntity(player1Id)?.get<ManaPoolComponent>()?.isEmpty shouldBe true
        }
    }

    context("card drawing") {
        test("draw card moves from library to hand") {
            var state = newGame()
            // Add a card to library
            val (cardId, state2) = state.createEntity(
                EntityId.generate(),
                CardComponent(bearDef, player1Id)
            )
            state = state2.addToZone(cardId, ZoneId.library(player1Id))

            val action = DrawCard(player1Id, 1)
            val result = handler.execute(state, action)

            result.shouldBeInstanceOf<GameActionResult.Success>()
            val success = result as GameActionResult.Success

            success.state.getZone(ZoneId.library(player1Id)).contains(cardId) shouldBe false
            success.state.getZone(ZoneId.hand(player1Id)).contains(cardId) shouldBe true

            success.events shouldHaveSize 1
            success.events[0].shouldBeInstanceOf<GameActionEvent.CardDrawn>()
        }

        test("drawing from empty library marks player as lost") {
            val state = newGame()  // Empty library by default
            val action = DrawCard(player1Id, 1)

            val result = handler.execute(state, action)

            result.shouldBeInstanceOf<GameActionResult.Success>()
            val success = result as GameActionResult.Success

            success.state.getEntity(player1Id)?.has<LostGameComponent>() shouldBe true
            success.events shouldContain GameActionEvent.DrawFailed(player1Id)
        }
    }

    context("tap/untap") {
        test("tap permanent adds TappedComponent") {
            val (bearId, state) = newGame().addBearToBattlefield(player1Id)
            val action = Tap(bearId)

            val result = handler.execute(state, action)

            result.shouldBeInstanceOf<GameActionResult.Success>()
            val success = result as GameActionResult.Success
            success.state.getEntity(bearId)?.has<TappedComponent>() shouldBe true
        }

        test("untap permanent removes TappedComponent") {
            val (bearId, state1) = newGame().addBearToBattlefield(player1Id)
            val state = state1.updateEntity(bearId) { it.with(TappedComponent) }

            val action = Untap(bearId)
            val result = handler.execute(state, action)

            result.shouldBeInstanceOf<GameActionResult.Success>()
            val success = result as GameActionResult.Success
            success.state.getEntity(bearId)?.has<TappedComponent>() shouldBe false
        }

        test("untap all untaps all controlled permanents") {
            val (bear1Id, state1) = newGame().addBearToBattlefield(player1Id)
            val (bear2Id, state2) = state1.addBearToBattlefield(player1Id)
            val state = state2
                .updateEntity(bear1Id) { it.with(TappedComponent) }
                .updateEntity(bear2Id) { it.with(TappedComponent) }

            val action = UntapAll(player1Id)
            val result = handler.execute(state, action)

            result.shouldBeInstanceOf<GameActionResult.Success>()
            val success = result as GameActionResult.Success
            success.state.getEntity(bear1Id)?.has<TappedComponent>() shouldBe false
            success.state.getEntity(bear2Id)?.has<TappedComponent>() shouldBe false
        }
    }

    context("destroy permanent") {
        test("destroy moves to graveyard") {
            val (bearId, state) = newGame().addBearToBattlefield(player1Id)
            val action = DestroyPermanent(bearId)

            val result = handler.execute(state, action)

            result.shouldBeInstanceOf<GameActionResult.Success>()
            val success = result as GameActionResult.Success

            success.state.getZone(ZoneId.BATTLEFIELD).contains(bearId) shouldBe false
            success.state.getZone(ZoneId.graveyard(player1Id)).contains(bearId) shouldBe true

            success.events.any { it is GameActionEvent.PermanentDestroyed } shouldBe true
            success.events.any { it is GameActionEvent.CreatureDied } shouldBe true
        }

        test("destroy clears damage") {
            val (bearId, state1) = newGame().addBearToBattlefield(player1Id)
            val state = state1.updateEntity(bearId) { it.with(DamageComponent(1)) }

            val action = DestroyPermanent(bearId)
            val result = handler.execute(state, action)

            result.shouldBeInstanceOf<GameActionResult.Success>()
            val success = result as GameActionResult.Success
            success.state.getEntity(bearId)?.has<DamageComponent>() shouldBe false
        }
    }

    context("counters") {
        test("add +1/+1 counters") {
            val (bearId, state) = newGame().addBearToBattlefield(player1Id)
            val action = AddCounters(bearId, "+1/+1", 2)

            val result = handler.execute(state, action)

            result.shouldBeInstanceOf<GameActionResult.Success>()
            val success = result as GameActionResult.Success
            val counters = success.state.getEntity(bearId)?.get<CountersComponent>()
            counters.shouldNotBeNull()
            counters.plusOnePlusOneCount shouldBe 2
        }

        test("add poison counters to player") {
            val state = newGame()
            val action = AddPoisonCounters(player1Id, 3)

            val result = handler.execute(state, action)

            result.shouldBeInstanceOf<GameActionResult.Success>()
            val success = result as GameActionResult.Success
            success.state.getEntity(player1Id)?.get<PoisonComponent>()?.counters shouldBe 3
        }
    }

    context("land playing") {
        test("play land moves to battlefield") {
            val (forestId, state) = newGame().addForestToHand(player1Id)
            val action = PlayLand(forestId, player1Id)

            val result = handler.execute(state, action)

            result.shouldBeInstanceOf<GameActionResult.Success>()
            val success = result as GameActionResult.Success

            success.state.getZone(ZoneId.hand(player1Id)).contains(forestId) shouldBe false
            success.state.getZone(ZoneId.BATTLEFIELD).contains(forestId) shouldBe true

            // Lands played count should increase
            success.state.getEntity(player1Id)?.get<LandsPlayedComponent>()?.count shouldBe 1
        }

        test("cannot play second land without effect") {
            val (forest1Id, state1) = newGame().addForestToHand(player1Id)
            val (forest2Id, state2) = state1.addForestToHand(player1Id)

            // Play first land
            val result1 = handler.execute(state2, PlayLand(forest1Id, player1Id))
            result1.shouldBeInstanceOf<GameActionResult.Success>()

            // Try to play second land
            val result2 = handler.execute((result1 as GameActionResult.Success).state, PlayLand(forest2Id, player1Id))
            result2.shouldBeInstanceOf<GameActionResult.Failure>()
        }
    }

    context("state-based actions") {
        test("creature with lethal damage dies") {
            val (bearId, state1) = newGame().addBearToBattlefield(player1Id)
            // 2/2 bear with 2 damage = lethal
            val state = state1.updateEntity(bearId) { it.with(DamageComponent(2)) }

            val action = CheckStateBasedActions()
            val result = handler.execute(state, action)

            result.shouldBeInstanceOf<GameActionResult.Success>()
            val success = result as GameActionResult.Success

            success.state.getZone(ZoneId.BATTLEFIELD).contains(bearId) shouldBe false
            success.state.getZone(ZoneId.graveyard(player1Id)).contains(bearId) shouldBe true
        }

        test("player at 0 life loses") {
            var state = newGame()
            state = state.updateEntity(player1Id) { c ->
                c.with(LifeComponent(0))
            }

            val action = CheckStateBasedActions()
            val result = handler.execute(state, action)

            result.shouldBeInstanceOf<GameActionResult.Success>()
            val success = result as GameActionResult.Success

            success.state.getEntity(player1Id)?.has<LostGameComponent>() shouldBe true
            success.events.any { it is GameActionEvent.PlayerLost } shouldBe true
        }

        test("player with 10 poison counters loses") {
            var state = newGame()
            state = state.updateEntity(player1Id) { c ->
                c.with(PoisonComponent(10))
            }

            val action = CheckStateBasedActions()
            val result = handler.execute(state, action)

            result.shouldBeInstanceOf<GameActionResult.Success>()
            val success = result as GameActionResult.Success

            success.state.getEntity(player1Id)?.has<LostGameComponent>() shouldBe true
        }

        test("game ends when only one player remains") {
            var state = newGame()
            state = state.updateEntity(player1Id) { c ->
                c.with(LostGameComponent("Test"))
            }

            val action = CheckStateBasedActions()
            val result = handler.execute(state, action)

            result.shouldBeInstanceOf<GameActionResult.Success>()
            val success = result as GameActionResult.Success

            success.state.isGameOver shouldBe true
            success.state.winner shouldBe player2Id
        }
    }

    context("execute all") {
        test("executes multiple actions in sequence") {
            val state = newGame()
            val actions = listOf(
                GainLife(player1Id, 5),
                LoseLife(player2Id, 3),
                AddMana(player1Id, Color.GREEN, 1)
            )

            val result = handler.executeAll(state, actions)

            result.shouldBeInstanceOf<GameActionResult.Success>()
            val success = result as GameActionResult.Success

            success.state.getEntity(player1Id)?.get<LifeComponent>()?.life shouldBe 25
            success.state.getEntity(player2Id)?.get<LifeComponent>()?.life shouldBe 17
            success.state.getEntity(player1Id)?.get<ManaPoolComponent>()?.pool?.green shouldBe 1

            success.events shouldHaveSize 3  // 1 LifeChanged + 1 LifeChanged + 1 ManaAdded
        }

        test("stops on first failure") {
            val state = newGame()
            val actions = listOf(
                GainLife(player1Id, 5),
                PlayLand(EntityId.of("nonexistent"), player1Id),  // Will fail
                GainLife(player2Id, 5)  // Should not execute
            )

            val result = handler.executeAll(state, actions)

            result.shouldBeInstanceOf<GameActionResult.Failure>()
        }
    }
})
