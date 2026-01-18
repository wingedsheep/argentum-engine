package com.wingedsheep.rulesengine.ecs.event

import com.wingedsheep.rulesengine.ecs.EntityId
import com.wingedsheep.rulesengine.ecs.ZoneId
import com.wingedsheep.rulesengine.ecs.action.EcsActionEvent
import com.wingedsheep.rulesengine.ecs.script.EcsEvent
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class EcsGameEventTest : FunSpec({

    val player1Id = EntityId.of("player1")
    val player2Id = EntityId.of("player2")
    val creatureId = EntityId.of("creature1")

    context("EcsGameEventConverter from action events") {

        test("LifeChanged with positive delta produces LifeGained") {
            val actionEvent = EcsActionEvent.LifeChanged(player1Id, 20, 25)

            val gameEvents = EcsGameEventConverter.fromActionEvent(actionEvent)

            gameEvents shouldHaveSize 1
            val event = gameEvents.first()
            event.shouldBeInstanceOf<EcsGameEvent.LifeGained>()
            event.playerId shouldBe player1Id
            event.amount shouldBe 5
            event.newTotal shouldBe 25
        }

        test("LifeChanged with negative delta produces LifeLost") {
            val actionEvent = EcsActionEvent.LifeChanged(player1Id, 20, 17)

            val gameEvents = EcsGameEventConverter.fromActionEvent(actionEvent)

            gameEvents shouldHaveSize 1
            val event = gameEvents.first()
            event.shouldBeInstanceOf<EcsGameEvent.LifeLost>()
            event.playerId shouldBe player1Id
            event.amount shouldBe 3
            event.newTotal shouldBe 17
        }

        test("LifeChanged with zero delta produces no events") {
            val actionEvent = EcsActionEvent.LifeChanged(player1Id, 20, 20)

            val gameEvents = EcsGameEventConverter.fromActionEvent(actionEvent)

            gameEvents.shouldBeEmpty()
        }

        test("DamageDealtToPlayer converts correctly") {
            val actionEvent = EcsActionEvent.DamageDealtToPlayer(creatureId, player2Id, 3)

            val gameEvents = EcsGameEventConverter.fromActionEvent(actionEvent)

            gameEvents shouldHaveSize 1
            val event = gameEvents.first()
            event.shouldBeInstanceOf<EcsGameEvent.DamageDealtToPlayer>()
            event.sourceId shouldBe creatureId
            event.targetPlayerId shouldBe player2Id
            event.amount shouldBe 3
        }

        test("DamageDealtToCreature converts correctly") {
            val targetId = EntityId.of("target")
            val actionEvent = EcsActionEvent.DamageDealtToCreature(creatureId, targetId, 2)

            val gameEvents = EcsGameEventConverter.fromActionEvent(actionEvent)

            gameEvents shouldHaveSize 1
            val event = gameEvents.first()
            event.shouldBeInstanceOf<EcsGameEvent.DamageDealtToCreature>()
            event.sourceId shouldBe creatureId
            event.targetCreatureId shouldBe targetId
            event.amount shouldBe 2
        }

        test("CardDrawn converts correctly") {
            val cardId = EntityId.of("card1")
            val actionEvent = EcsActionEvent.CardDrawn(player1Id, cardId, "Forest")

            val gameEvents = EcsGameEventConverter.fromActionEvent(actionEvent)

            gameEvents shouldHaveSize 1
            val event = gameEvents.first()
            event.shouldBeInstanceOf<EcsGameEvent.CardDrawn>()
            event.playerId shouldBe player1Id
            event.cardId shouldBe cardId
            event.cardName shouldBe "Forest"
        }

        test("CardDiscarded converts correctly") {
            val cardId = EntityId.of("card1")
            val actionEvent = EcsActionEvent.CardDiscarded(player1Id, cardId, "Island")

            val gameEvents = EcsGameEventConverter.fromActionEvent(actionEvent)

            gameEvents shouldHaveSize 1
            val event = gameEvents.first()
            event.shouldBeInstanceOf<EcsGameEvent.CardDiscarded>()
            event.playerId shouldBe player1Id
            event.cardId shouldBe cardId
            event.cardName shouldBe "Island"
        }

        test("CreatureDied converts correctly") {
            val actionEvent = EcsActionEvent.CreatureDied(creatureId, "Grizzly Bears", player1Id)

            val gameEvents = EcsGameEventConverter.fromActionEvent(actionEvent)

            gameEvents shouldHaveSize 1
            val event = gameEvents.first()
            event.shouldBeInstanceOf<EcsGameEvent.CreatureDied>()
            event.entityId shouldBe creatureId
            event.cardName shouldBe "Grizzly Bears"
            event.ownerId shouldBe player1Id
        }

        test("CardExiled converts correctly") {
            val actionEvent = EcsActionEvent.CardExiled(creatureId, "Pacifism")

            val gameEvents = EcsGameEventConverter.fromActionEvent(actionEvent)

            gameEvents shouldHaveSize 1
            val event = gameEvents.first()
            event.shouldBeInstanceOf<EcsGameEvent.CardExiled>()
            event.entityId shouldBe creatureId
            event.cardName shouldBe "Pacifism"
        }

        test("CombatStarted converts correctly") {
            val actionEvent = EcsActionEvent.CombatStarted(player1Id, player2Id)

            val gameEvents = EcsGameEventConverter.fromActionEvent(actionEvent)

            gameEvents shouldHaveSize 1
            val event = gameEvents.first()
            event.shouldBeInstanceOf<EcsGameEvent.CombatBegan>()
            event.attackingPlayerId shouldBe player1Id
            event.defendingPlayerId shouldBe player2Id
        }

        test("BlockerDeclared converts correctly") {
            val blockerId = EntityId.of("blocker")
            val attackerId = EntityId.of("attacker")
            val actionEvent = EcsActionEvent.BlockerDeclared(blockerId, attackerId, "Wall of Stone")

            val gameEvents = EcsGameEventConverter.fromActionEvent(actionEvent)

            gameEvents shouldHaveSize 1
            val event = gameEvents.first()
            event.shouldBeInstanceOf<EcsGameEvent.BlockerDeclared>()
            event.blockerId shouldBe blockerId
            event.attackerId shouldBe attackerId
            event.blockerName shouldBe "Wall of Stone"
        }

        test("PlayerLost converts correctly") {
            val actionEvent = EcsActionEvent.PlayerLost(player1Id, "0 life")

            val gameEvents = EcsGameEventConverter.fromActionEvent(actionEvent)

            gameEvents shouldHaveSize 1
            val event = gameEvents.first()
            event.shouldBeInstanceOf<EcsGameEvent.PlayerLost>()
            event.playerId shouldBe player1Id
            event.reason shouldBe "0 life"
        }

        test("GameEnded converts correctly") {
            val actionEvent = EcsActionEvent.GameEnded(player2Id)

            val gameEvents = EcsGameEventConverter.fromActionEvent(actionEvent)

            gameEvents shouldHaveSize 1
            val event = gameEvents.first()
            event.shouldBeInstanceOf<EcsGameEvent.GameEnded>()
            event.winnerId shouldBe player2Id
        }

        test("ManaAdded produces no game events") {
            val actionEvent = EcsActionEvent.ManaAdded(player1Id, "Green", 1)

            val gameEvents = EcsGameEventConverter.fromActionEvent(actionEvent)

            gameEvents.shouldBeEmpty()
        }

        test("LandPlayed produces no game events") {
            val landId = EntityId.of("land1")
            val actionEvent = EcsActionEvent.LandPlayed(player1Id, landId, "Forest")

            val gameEvents = EcsGameEventConverter.fromActionEvent(actionEvent)

            gameEvents.shouldBeEmpty()
        }
    }

    context("EcsGameEventConverter from effect events") {

        test("LifeGained converts correctly") {
            val effectEvent = EcsEvent.LifeGained(player1Id, 3)

            val gameEvents = EcsGameEventConverter.fromEffectEvent(effectEvent)

            gameEvents shouldHaveSize 1
            val event = gameEvents.first()
            event.shouldBeInstanceOf<EcsGameEvent.LifeGained>()
            event.playerId shouldBe player1Id
            event.amount shouldBe 3
        }

        test("LifeLost converts correctly") {
            val effectEvent = EcsEvent.LifeLost(player1Id, 4)

            val gameEvents = EcsGameEventConverter.fromEffectEvent(effectEvent)

            gameEvents shouldHaveSize 1
            val event = gameEvents.first()
            event.shouldBeInstanceOf<EcsGameEvent.LifeLost>()
            event.playerId shouldBe player1Id
            event.amount shouldBe 4
        }

        test("CountersAdded converts correctly") {
            val effectEvent = EcsEvent.CountersAdded(creatureId, "+1/+1", 2)

            val gameEvents = EcsGameEventConverter.fromEffectEvent(effectEvent)

            gameEvents shouldHaveSize 1
            val event = gameEvents.first()
            event.shouldBeInstanceOf<EcsGameEvent.CountersAdded>()
            event.entityId shouldBe creatureId
            event.counterType shouldBe "+1/+1"
            event.count shouldBe 2
        }

        test("TokenCreated produces no game events") {
            val effectEvent = EcsEvent.TokenCreated(player1Id, 2, "1/1 white Soldier")

            val gameEvents = EcsGameEventConverter.fromEffectEvent(effectEvent)

            gameEvents.shouldBeEmpty()
        }
    }
})
