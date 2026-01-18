package com.wingedsheep.rulesengine.ecs.action

import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.core.Color
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype
import com.wingedsheep.rulesengine.ecs.EcsGameState
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

class EcsActionHandlerTest : FunSpec({

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

    fun newGame(): EcsGameState = EcsGameState.newGame(
        listOf(player1Id to "Alice", player2Id to "Bob")
    )

    fun EcsGameState.addBearToHand(playerId: EntityId): Pair<EntityId, EcsGameState> {
        val (bearId, state1) = createEntity(
            EntityId.generate(),
            CardComponent(bearDef, playerId),
            ControllerComponent(playerId)
        )
        return bearId to state1.addToZone(bearId, ZoneId.hand(playerId))
    }

    fun EcsGameState.addBearToBattlefield(controllerId: EntityId): Pair<EntityId, EcsGameState> {
        val (bearId, state1) = createEntity(
            EntityId.generate(),
            CardComponent(bearDef, controllerId),
            ControllerComponent(controllerId)
        )
        return bearId to state1.addToZone(bearId, ZoneId.BATTLEFIELD)
    }

    fun EcsGameState.addForestToHand(playerId: EntityId): Pair<EntityId, EcsGameState> {
        val (forestId, state1) = createEntity(
            EntityId.generate(),
            CardComponent(forestDef, playerId),
            ControllerComponent(playerId)
        )
        return forestId to state1.addToZone(forestId, ZoneId.hand(playerId))
    }

    val handler = EcsActionHandler()

    context("life actions") {
        test("gain life increases life total") {
            val state = newGame()
            val action = EcsGainLife(player1Id, 5)

            val result = handler.execute(state, action)

            result.shouldBeInstanceOf<EcsActionResult.Success>()
            val success = result as EcsActionResult.Success
            success.state.getEntity(player1Id)?.get<LifeComponent>()?.life shouldBe 25

            success.events shouldHaveSize 1
            success.events[0].shouldBeInstanceOf<EcsActionEvent.LifeChanged>()
        }

        test("lose life decreases life total") {
            val state = newGame()
            val action = EcsLoseLife(player1Id, 3)

            val result = handler.execute(state, action)

            result.shouldBeInstanceOf<EcsActionResult.Success>()
            val success = result as EcsActionResult.Success
            success.state.getEntity(player1Id)?.get<LifeComponent>()?.life shouldBe 17
        }

        test("deal damage to player decreases life") {
            val state = newGame()
            val action = EcsDealDamageToPlayer(player2Id, 4, player1Id)

            val result = handler.execute(state, action)

            result.shouldBeInstanceOf<EcsActionResult.Success>()
            val success = result as EcsActionResult.Success
            success.state.getEntity(player2Id)?.get<LifeComponent>()?.life shouldBe 16

            success.events shouldHaveSize 2
            success.events[0].shouldBeInstanceOf<EcsActionEvent.DamageDealtToPlayer>()
            success.events[1].shouldBeInstanceOf<EcsActionEvent.LifeChanged>()
        }
    }

    context("mana actions") {
        test("add colored mana") {
            val state = newGame()
            val action = EcsAddMana(player1Id, Color.GREEN, 2)

            val result = handler.execute(state, action)

            result.shouldBeInstanceOf<EcsActionResult.Success>()
            val success = result as EcsActionResult.Success
            success.state.getEntity(player1Id)?.get<ManaPoolComponent>()?.pool?.green shouldBe 2
        }

        test("add colorless mana") {
            val state = newGame()
            val action = EcsAddColorlessMana(player1Id, 3)

            val result = handler.execute(state, action)

            result.shouldBeInstanceOf<EcsActionResult.Success>()
            val success = result as EcsActionResult.Success
            success.state.getEntity(player1Id)?.get<ManaPoolComponent>()?.pool?.colorless shouldBe 3
        }

        test("empty mana pool") {
            // First add mana, then empty
            var state = newGame()
            state = (handler.execute(state, EcsAddMana(player1Id, Color.GREEN, 2)) as EcsActionResult.Success).state

            val result = handler.execute(state, EcsEmptyManaPool(player1Id))

            result.shouldBeInstanceOf<EcsActionResult.Success>()
            val success = result as EcsActionResult.Success
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

            val action = EcsDrawCard(player1Id, 1)
            val result = handler.execute(state, action)

            result.shouldBeInstanceOf<EcsActionResult.Success>()
            val success = result as EcsActionResult.Success

            success.state.getZone(ZoneId.library(player1Id)).contains(cardId) shouldBe false
            success.state.getZone(ZoneId.hand(player1Id)).contains(cardId) shouldBe true

            success.events shouldHaveSize 1
            success.events[0].shouldBeInstanceOf<EcsActionEvent.CardDrawn>()
        }

        test("drawing from empty library marks player as lost") {
            val state = newGame()  // Empty library by default
            val action = EcsDrawCard(player1Id, 1)

            val result = handler.execute(state, action)

            result.shouldBeInstanceOf<EcsActionResult.Success>()
            val success = result as EcsActionResult.Success

            success.state.getEntity(player1Id)?.has<LostGameComponent>() shouldBe true
            success.events shouldContain EcsActionEvent.DrawFailed(player1Id)
        }
    }

    context("tap/untap") {
        test("tap permanent adds TappedComponent") {
            val (bearId, state) = newGame().addBearToBattlefield(player1Id)
            val action = EcsTap(bearId)

            val result = handler.execute(state, action)

            result.shouldBeInstanceOf<EcsActionResult.Success>()
            val success = result as EcsActionResult.Success
            success.state.getEntity(bearId)?.has<TappedComponent>() shouldBe true
        }

        test("untap permanent removes TappedComponent") {
            val (bearId, state1) = newGame().addBearToBattlefield(player1Id)
            val state = state1.updateEntity(bearId) { it.with(TappedComponent) }

            val action = EcsUntap(bearId)
            val result = handler.execute(state, action)

            result.shouldBeInstanceOf<EcsActionResult.Success>()
            val success = result as EcsActionResult.Success
            success.state.getEntity(bearId)?.has<TappedComponent>() shouldBe false
        }

        test("untap all untaps all controlled permanents") {
            val (bear1Id, state1) = newGame().addBearToBattlefield(player1Id)
            val (bear2Id, state2) = state1.addBearToBattlefield(player1Id)
            val state = state2
                .updateEntity(bear1Id) { it.with(TappedComponent) }
                .updateEntity(bear2Id) { it.with(TappedComponent) }

            val action = EcsUntapAll(player1Id)
            val result = handler.execute(state, action)

            result.shouldBeInstanceOf<EcsActionResult.Success>()
            val success = result as EcsActionResult.Success
            success.state.getEntity(bear1Id)?.has<TappedComponent>() shouldBe false
            success.state.getEntity(bear2Id)?.has<TappedComponent>() shouldBe false
        }
    }

    context("destroy permanent") {
        test("destroy moves to graveyard") {
            val (bearId, state) = newGame().addBearToBattlefield(player1Id)
            val action = EcsDestroyPermanent(bearId)

            val result = handler.execute(state, action)

            result.shouldBeInstanceOf<EcsActionResult.Success>()
            val success = result as EcsActionResult.Success

            success.state.getZone(ZoneId.BATTLEFIELD).contains(bearId) shouldBe false
            success.state.getZone(ZoneId.graveyard(player1Id)).contains(bearId) shouldBe true

            success.events.any { it is EcsActionEvent.PermanentDestroyed } shouldBe true
            success.events.any { it is EcsActionEvent.CreatureDied } shouldBe true
        }

        test("destroy clears damage") {
            val (bearId, state1) = newGame().addBearToBattlefield(player1Id)
            val state = state1.updateEntity(bearId) { it.with(DamageComponent(1)) }

            val action = EcsDestroyPermanent(bearId)
            val result = handler.execute(state, action)

            result.shouldBeInstanceOf<EcsActionResult.Success>()
            val success = result as EcsActionResult.Success
            success.state.getEntity(bearId)?.has<DamageComponent>() shouldBe false
        }
    }

    context("counters") {
        test("add +1/+1 counters") {
            val (bearId, state) = newGame().addBearToBattlefield(player1Id)
            val action = EcsAddCounters(bearId, "+1/+1", 2)

            val result = handler.execute(state, action)

            result.shouldBeInstanceOf<EcsActionResult.Success>()
            val success = result as EcsActionResult.Success
            val counters = success.state.getEntity(bearId)?.get<CountersComponent>()
            counters.shouldNotBeNull()
            counters.plusOnePlusOneCount shouldBe 2
        }

        test("add poison counters to player") {
            val state = newGame()
            val action = EcsAddPoisonCounters(player1Id, 3)

            val result = handler.execute(state, action)

            result.shouldBeInstanceOf<EcsActionResult.Success>()
            val success = result as EcsActionResult.Success
            success.state.getEntity(player1Id)?.get<PoisonComponent>()?.counters shouldBe 3
        }
    }

    context("land playing") {
        test("play land moves to battlefield") {
            val (forestId, state) = newGame().addForestToHand(player1Id)
            val action = EcsPlayLand(forestId, player1Id)

            val result = handler.execute(state, action)

            result.shouldBeInstanceOf<EcsActionResult.Success>()
            val success = result as EcsActionResult.Success

            success.state.getZone(ZoneId.hand(player1Id)).contains(forestId) shouldBe false
            success.state.getZone(ZoneId.BATTLEFIELD).contains(forestId) shouldBe true

            // Lands played count should increase
            success.state.getEntity(player1Id)?.get<LandsPlayedComponent>()?.count shouldBe 1
        }

        test("cannot play second land without effect") {
            val (forest1Id, state1) = newGame().addForestToHand(player1Id)
            val (forest2Id, state2) = state1.addForestToHand(player1Id)

            // Play first land
            val result1 = handler.execute(state2, EcsPlayLand(forest1Id, player1Id))
            result1.shouldBeInstanceOf<EcsActionResult.Success>()

            // Try to play second land
            val result2 = handler.execute((result1 as EcsActionResult.Success).state, EcsPlayLand(forest2Id, player1Id))
            result2.shouldBeInstanceOf<EcsActionResult.Failure>()
        }
    }

    context("state-based actions") {
        test("creature with lethal damage dies") {
            val (bearId, state1) = newGame().addBearToBattlefield(player1Id)
            // 2/2 bear with 2 damage = lethal
            val state = state1.updateEntity(bearId) { it.with(DamageComponent(2)) }

            val action = EcsCheckStateBasedActions()
            val result = handler.execute(state, action)

            result.shouldBeInstanceOf<EcsActionResult.Success>()
            val success = result as EcsActionResult.Success

            success.state.getZone(ZoneId.BATTLEFIELD).contains(bearId) shouldBe false
            success.state.getZone(ZoneId.graveyard(player1Id)).contains(bearId) shouldBe true
        }

        test("player at 0 life loses") {
            var state = newGame()
            state = state.updateEntity(player1Id) { c ->
                c.with(LifeComponent(0))
            }

            val action = EcsCheckStateBasedActions()
            val result = handler.execute(state, action)

            result.shouldBeInstanceOf<EcsActionResult.Success>()
            val success = result as EcsActionResult.Success

            success.state.getEntity(player1Id)?.has<LostGameComponent>() shouldBe true
            success.events.any { it is EcsActionEvent.PlayerLost } shouldBe true
        }

        test("player with 10 poison counters loses") {
            var state = newGame()
            state = state.updateEntity(player1Id) { c ->
                c.with(PoisonComponent(10))
            }

            val action = EcsCheckStateBasedActions()
            val result = handler.execute(state, action)

            result.shouldBeInstanceOf<EcsActionResult.Success>()
            val success = result as EcsActionResult.Success

            success.state.getEntity(player1Id)?.has<LostGameComponent>() shouldBe true
        }

        test("game ends when only one player remains") {
            var state = newGame()
            state = state.updateEntity(player1Id) { c ->
                c.with(LostGameComponent("Test"))
            }

            val action = EcsCheckStateBasedActions()
            val result = handler.execute(state, action)

            result.shouldBeInstanceOf<EcsActionResult.Success>()
            val success = result as EcsActionResult.Success

            success.state.isGameOver shouldBe true
            success.state.winner shouldBe player2Id
        }
    }

    context("execute all") {
        test("executes multiple actions in sequence") {
            val state = newGame()
            val actions = listOf(
                EcsGainLife(player1Id, 5),
                EcsLoseLife(player2Id, 3),
                EcsAddMana(player1Id, Color.GREEN, 1)
            )

            val result = handler.executeAll(state, actions)

            result.shouldBeInstanceOf<EcsActionResult.Success>()
            val success = result as EcsActionResult.Success

            success.state.getEntity(player1Id)?.get<LifeComponent>()?.life shouldBe 25
            success.state.getEntity(player2Id)?.get<LifeComponent>()?.life shouldBe 17
            success.state.getEntity(player1Id)?.get<ManaPoolComponent>()?.pool?.green shouldBe 1

            success.events shouldHaveSize 3  // 1 LifeChanged + 1 LifeChanged + 1 ManaAdded
        }

        test("stops on first failure") {
            val state = newGame()
            val actions = listOf(
                EcsGainLife(player1Id, 5),
                EcsPlayLand(EntityId.of("nonexistent"), player1Id),  // Will fail
                EcsGainLife(player2Id, 5)  // Should not execute
            )

            val result = handler.executeAll(state, actions)

            result.shouldBeInstanceOf<EcsActionResult.Failure>()
        }
    }
})
