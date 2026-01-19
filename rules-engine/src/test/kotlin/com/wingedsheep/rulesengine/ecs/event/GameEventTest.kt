package com.wingedsheep.rulesengine.ecs.event

import com.wingedsheep.rulesengine.ecs.EntityId
import com.wingedsheep.rulesengine.ecs.ZoneId
import com.wingedsheep.rulesengine.ecs.action.GameActionEvent
import com.wingedsheep.rulesengine.ecs.script.EffectEvent
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class GameEventTest : FunSpec({

    val player1Id = EntityId.of("player1")
    val player2Id = EntityId.of("player2")
    val creatureId = EntityId.of("creature1")

    context("GameEventConverter from action events") {

        test("LifeChanged with positive delta produces LifeGained") {
            val actionEvent = GameActionEvent.LifeChanged(player1Id, 20, 25)

            val gameEvents = GameEventConverter.fromActionEvent(actionEvent)

            gameEvents shouldHaveSize 1
            val event = gameEvents.first()
            event.shouldBeInstanceOf<GameEvent.LifeGained>()
            event.playerId shouldBe player1Id
            event.amount shouldBe 5
            event.newTotal shouldBe 25
        }

        test("LifeChanged with negative delta produces LifeLost") {
            val actionEvent = GameActionEvent.LifeChanged(player1Id, 20, 17)

            val gameEvents = GameEventConverter.fromActionEvent(actionEvent)

            gameEvents shouldHaveSize 1
            val event = gameEvents.first()
            event.shouldBeInstanceOf<GameEvent.LifeLost>()
            event.playerId shouldBe player1Id
            event.amount shouldBe 3
            event.newTotal shouldBe 17
        }

        test("LifeChanged with zero delta produces no events") {
            val actionEvent = GameActionEvent.LifeChanged(player1Id, 20, 20)

            val gameEvents = GameEventConverter.fromActionEvent(actionEvent)

            gameEvents.shouldBeEmpty()
        }

        test("DamageDealtToPlayer converts correctly") {
            val actionEvent = GameActionEvent.DamageDealtToPlayer(creatureId, player2Id, 3)

            val gameEvents = GameEventConverter.fromActionEvent(actionEvent)

            gameEvents shouldHaveSize 1
            val event = gameEvents.first()
            event.shouldBeInstanceOf<GameEvent.DamageDealtToPlayer>()
            event.sourceId shouldBe creatureId
            event.targetPlayerId shouldBe player2Id
            event.amount shouldBe 3
        }

        test("DamageDealtToCreature converts correctly") {
            val targetId = EntityId.of("target")
            val actionEvent = GameActionEvent.DamageDealtToCreature(creatureId, targetId, 2)

            val gameEvents = GameEventConverter.fromActionEvent(actionEvent)

            gameEvents shouldHaveSize 1
            val event = gameEvents.first()
            event.shouldBeInstanceOf<GameEvent.DamageDealtToCreature>()
            event.sourceId shouldBe creatureId
            event.targetCreatureId shouldBe targetId
            event.amount shouldBe 2
        }

        test("CardDrawn converts correctly") {
            val cardId = EntityId.of("card1")
            val actionEvent = GameActionEvent.CardDrawn(player1Id, cardId, "Forest")

            val gameEvents = GameEventConverter.fromActionEvent(actionEvent)

            gameEvents shouldHaveSize 1
            val event = gameEvents.first()
            event.shouldBeInstanceOf<GameEvent.CardDrawn>()
            event.playerId shouldBe player1Id
            event.cardId shouldBe cardId
            event.cardName shouldBe "Forest"
        }

        test("CardDiscarded converts correctly") {
            val cardId = EntityId.of("card1")
            val actionEvent = GameActionEvent.CardDiscarded(player1Id, cardId, "Island")

            val gameEvents = GameEventConverter.fromActionEvent(actionEvent)

            gameEvents shouldHaveSize 1
            val event = gameEvents.first()
            event.shouldBeInstanceOf<GameEvent.CardDiscarded>()
            event.playerId shouldBe player1Id
            event.cardId shouldBe cardId
            event.cardName shouldBe "Island"
        }

        test("CreatureDied converts correctly") {
            val actionEvent = GameActionEvent.CreatureDied(creatureId, "Grizzly Bears", player1Id)

            val gameEvents = GameEventConverter.fromActionEvent(actionEvent)

            gameEvents shouldHaveSize 1
            val event = gameEvents.first()
            event.shouldBeInstanceOf<GameEvent.CreatureDied>()
            event.entityId shouldBe creatureId
            event.cardName shouldBe "Grizzly Bears"
            event.ownerId shouldBe player1Id
        }

        test("CardExiled converts correctly") {
            val actionEvent = GameActionEvent.CardExiled(creatureId, "Pacifism")

            val gameEvents = GameEventConverter.fromActionEvent(actionEvent)

            gameEvents shouldHaveSize 1
            val event = gameEvents.first()
            event.shouldBeInstanceOf<GameEvent.CardExiled>()
            event.entityId shouldBe creatureId
            event.cardName shouldBe "Pacifism"
        }

        test("CombatStarted converts correctly") {
            val actionEvent = GameActionEvent.CombatStarted(player1Id, player2Id)

            val gameEvents = GameEventConverter.fromActionEvent(actionEvent)

            gameEvents shouldHaveSize 1
            val event = gameEvents.first()
            event.shouldBeInstanceOf<GameEvent.CombatBegan>()
            event.attackingPlayerId shouldBe player1Id
            event.defendingPlayerId shouldBe player2Id
        }

        test("BlockerDeclared converts correctly") {
            val blockerId = EntityId.of("blocker")
            val attackerId = EntityId.of("attacker")
            val actionEvent = GameActionEvent.BlockerDeclared(blockerId, attackerId, "Wall of Stone")

            val gameEvents = GameEventConverter.fromActionEvent(actionEvent)

            gameEvents shouldHaveSize 1
            val event = gameEvents.first()
            event.shouldBeInstanceOf<GameEvent.BlockerDeclared>()
            event.blockerId shouldBe blockerId
            event.attackerId shouldBe attackerId
            event.blockerName shouldBe "Wall of Stone"
        }

        test("PlayerLost converts correctly") {
            val actionEvent = GameActionEvent.PlayerLost(player1Id, "0 life")

            val gameEvents = GameEventConverter.fromActionEvent(actionEvent)

            gameEvents shouldHaveSize 1
            val event = gameEvents.first()
            event.shouldBeInstanceOf<GameEvent.PlayerLost>()
            event.playerId shouldBe player1Id
            event.reason shouldBe "0 life"
        }

        test("GameEnded converts correctly") {
            val actionEvent = GameActionEvent.GameEnded(player2Id)

            val gameEvents = GameEventConverter.fromActionEvent(actionEvent)

            gameEvents shouldHaveSize 1
            val event = gameEvents.first()
            event.shouldBeInstanceOf<GameEvent.GameEnded>()
            event.winnerId shouldBe player2Id
        }

        test("ManaAdded produces no game events") {
            val actionEvent = GameActionEvent.ManaAdded(player1Id, "Green", 1)

            val gameEvents = GameEventConverter.fromActionEvent(actionEvent)

            gameEvents.shouldBeEmpty()
        }

        test("LandPlayed produces no game events") {
            val landId = EntityId.of("land1")
            val actionEvent = GameActionEvent.LandPlayed(player1Id, landId, "Forest")

            val gameEvents = GameEventConverter.fromActionEvent(actionEvent)

            gameEvents.shouldBeEmpty()
        }
    }

    context("GameEventConverter from effect events") {

        test("LifeGained converts correctly") {
            val effectEvent = EffectEvent.LifeGained(player1Id, 3)

            val gameEvents = GameEventConverter.fromEffectEvent(effectEvent)

            gameEvents shouldHaveSize 1
            val event = gameEvents.first()
            event.shouldBeInstanceOf<GameEvent.LifeGained>()
            event.playerId shouldBe player1Id
            event.amount shouldBe 3
        }

        test("LifeLost converts correctly") {
            val effectEvent = EffectEvent.LifeLost(player1Id, 4)

            val gameEvents = GameEventConverter.fromEffectEvent(effectEvent)

            gameEvents shouldHaveSize 1
            val event = gameEvents.first()
            event.shouldBeInstanceOf<GameEvent.LifeLost>()
            event.playerId shouldBe player1Id
            event.amount shouldBe 4
        }

        test("CountersAdded converts correctly") {
            val effectEvent = EffectEvent.CountersAdded(creatureId, "+1/+1", 2)

            val gameEvents = GameEventConverter.fromEffectEvent(effectEvent)

            gameEvents shouldHaveSize 1
            val event = gameEvents.first()
            event.shouldBeInstanceOf<GameEvent.CountersAdded>()
            event.entityId shouldBe creatureId
            event.counterType shouldBe "+1/+1"
            event.count shouldBe 2
        }

        test("TokenCreated produces no game events") {
            val effectEvent = EffectEvent.TokenCreated(player1Id, 2, "1/1 white Soldier")

            val gameEvents = GameEventConverter.fromEffectEvent(effectEvent)

            gameEvents.shouldBeEmpty()
        }
    }
})
