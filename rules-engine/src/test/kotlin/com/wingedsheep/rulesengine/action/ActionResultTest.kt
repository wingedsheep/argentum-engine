package com.wingedsheep.rulesengine.action

import com.wingedsheep.rulesengine.game.GameState
import com.wingedsheep.rulesengine.player.Player
import com.wingedsheep.rulesengine.player.PlayerId
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe

class ActionResultTest : FunSpec({

    val player1Id = PlayerId.of("player1")
    val player2Id = PlayerId.of("player2")

    fun createPlayer1() = Player.create(player1Id, "Alice")
    fun createPlayer2() = Player.create(player2Id, "Bob")
    fun createState() = GameState.newGame(createPlayer1(), createPlayer2())

    context("ActionResult.Success") {
        test("isSuccess returns true") {
            val result = ActionResult.Success(
                state = createState(),
                action = GainLife(player1Id, 5)
            )

            result.isSuccess.shouldBeTrue()
            result.isFailure.shouldBeFalse()
        }

        test("getOrThrow returns state") {
            val state = createState()
            val result = ActionResult.Success(
                state = state,
                action = GainLife(player1Id, 5)
            )

            result.getOrThrow() shouldBe state
        }

        test("events defaults to empty list") {
            val result = ActionResult.Success(
                state = createState(),
                action = GainLife(player1Id, 5)
            )

            result.events shouldBe emptyList()
        }

        test("events can contain multiple events") {
            val events = listOf(
                GameEvent.LifeChanged("player1", 20, 25, 5),
                GameEvent.ManaAdded("player1", "Green", 1)
            )
            val result = ActionResult.Success(
                state = createState(),
                action = GainLife(player1Id, 5),
                events = events
            )

            result.events shouldBe events
        }
    }

    context("ActionResult.Failure") {
        test("isFailure returns true") {
            val result = ActionResult.Failure(
                state = createState(),
                action = GainLife(player1Id, 5),
                reason = "Test failure"
            )

            result.isFailure.shouldBeTrue()
            result.isSuccess.shouldBeFalse()
        }

        test("getOrThrow throws exception") {
            val result = ActionResult.Failure(
                state = createState(),
                action = GainLife(player1Id, 5),
                reason = "Test failure"
            )

            val exception = shouldThrow<IllegalStateException> {
                result.getOrThrow()
            }
            exception.message shouldBe "Action failed: Test failure"
        }

        test("reason is preserved") {
            val result = ActionResult.Failure(
                state = createState(),
                action = GainLife(player1Id, 5),
                reason = "Cannot gain life while dead"
            )

            result.reason shouldBe "Cannot gain life while dead"
        }
    }

    context("GameEvent types") {
        test("LifeChanged stores all values") {
            val event = GameEvent.LifeChanged(
                playerId = "player1",
                oldLife = 20,
                newLife = 25,
                delta = 5
            )

            event.playerId shouldBe "player1"
            event.oldLife shouldBe 20
            event.newLife shouldBe 25
            event.delta shouldBe 5
        }

        test("CardDrawn stores all values") {
            val event = GameEvent.CardDrawn(
                playerId = "player1",
                cardId = "card-123",
                cardName = "Grizzly Bears"
            )

            event.playerId shouldBe "player1"
            event.cardId shouldBe "card-123"
            event.cardName shouldBe "Grizzly Bears"
        }

        test("CardMoved stores all values") {
            val event = GameEvent.CardMoved(
                cardId = "card-123",
                cardName = "Grizzly Bears",
                fromZone = "HAND",
                toZone = "BATTLEFIELD"
            )

            event.cardId shouldBe "card-123"
            event.fromZone shouldBe "HAND"
            event.toZone shouldBe "BATTLEFIELD"
        }

        test("CardTapped stores all values") {
            val event = GameEvent.CardTapped(
                cardId = "card-123",
                cardName = "Forest"
            )

            event.cardId shouldBe "card-123"
            event.cardName shouldBe "Forest"
        }

        test("CardUntapped stores all values") {
            val event = GameEvent.CardUntapped(
                cardId = "card-123",
                cardName = "Forest"
            )

            event.cardId shouldBe "card-123"
            event.cardName shouldBe "Forest"
        }

        test("ManaAdded stores all values") {
            val event = GameEvent.ManaAdded(
                playerId = "player1",
                color = "Green",
                amount = 2
            )

            event.playerId shouldBe "player1"
            event.color shouldBe "Green"
            event.amount shouldBe 2
        }

        test("DamageDealt stores all values") {
            val event = GameEvent.DamageDealt(
                sourceId = "card-123",
                targetId = "player1",
                amount = 5,
                isPlayer = true
            )

            event.sourceId shouldBe "card-123"
            event.targetId shouldBe "player1"
            event.amount shouldBe 5
            event.isPlayer shouldBe true
        }

        test("DamageDealt allows null source") {
            val event = GameEvent.DamageDealt(
                sourceId = null,
                targetId = "player1",
                amount = 5,
                isPlayer = true
            )

            event.sourceId shouldBe null
        }

        test("CreatureDied stores all values") {
            val event = GameEvent.CreatureDied(
                cardId = "card-123",
                cardName = "Grizzly Bears",
                ownerId = "player1"
            )

            event.cardId shouldBe "card-123"
            event.cardName shouldBe "Grizzly Bears"
            event.ownerId shouldBe "player1"
        }

        test("PlayerLost stores all values") {
            val event = GameEvent.PlayerLost(
                playerId = "player1",
                reason = "Life total reached 0"
            )

            event.playerId shouldBe "player1"
            event.reason shouldBe "Life total reached 0"
        }

        test("GameEnded stores winner") {
            val event = GameEvent.GameEnded(winnerId = "player1")
            event.winnerId shouldBe "player1"
        }

        test("GameEnded allows null winner for draw") {
            val event = GameEvent.GameEnded(winnerId = null)
            event.winnerId shouldBe null
        }
    }
})
