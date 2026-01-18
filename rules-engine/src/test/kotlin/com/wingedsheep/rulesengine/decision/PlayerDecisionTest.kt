package com.wingedsheep.rulesengine.decision

import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.CardInstance
import com.wingedsheep.rulesengine.core.CardId
import com.wingedsheep.rulesengine.core.Color
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype
import com.wingedsheep.rulesengine.game.GameState
import com.wingedsheep.rulesengine.player.Player
import com.wingedsheep.rulesengine.player.PlayerId
import com.wingedsheep.rulesengine.targeting.Target
import com.wingedsheep.rulesengine.targeting.TargetCreature
import com.wingedsheep.rulesengine.targeting.TargetPlayer
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class PlayerDecisionTest : FunSpec({

    val player1Id = PlayerId.of("player1")
    val player2Id = PlayerId.of("player2")

    fun createPlayer1() = Player.create(player1Id, "Alice")
    fun createPlayer2() = Player.create(player2Id, "Bob")

    val bearDef = CardDefinition.creature(
        name = "Grizzly Bears",
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = setOf(Subtype.BEAST),
        power = 2,
        toughness = 2
    )

    context("ScriptedPlayerInterface") {
        test("returns scripted responses in order") {
            val state = GameState.newGame(createPlayer1(), createPlayer2())
            val playerInterface = ScriptedPlayerInterface.withResponses(
                YesNoChoice(true),
                YesNoChoice(false),
                NumberChoice(5)
            )

            val decision1 = YesNoDecision(player1Id, "Test decision 1")
            val decision2 = YesNoDecision(player1Id, "Test decision 2")
            val decision3 = ChooseNumber(player1Id, "Choose a number", 0, 10)

            val response1 = playerInterface.requestDecision(state, decision1)
            val response2 = playerInterface.requestDecision(state, decision2)
            val response3 = playerInterface.requestDecision(state, decision3)

            response1.shouldBeInstanceOf<YesNoChoice>()
            (response1 as YesNoChoice).choice shouldBe true

            response2.shouldBeInstanceOf<YesNoChoice>()
            (response2 as YesNoChoice).choice shouldBe false

            response3.shouldBeInstanceOf<NumberChoice>()
            (response3 as NumberChoice).number shouldBe 5
        }

        test("remaining responses count is correct") {
            val playerInterface = ScriptedPlayerInterface.withResponses(
                YesNoChoice(true),
                YesNoChoice(false)
            )

            playerInterface.remainingResponses shouldBe 2

            val state = GameState.newGame(createPlayer1(), createPlayer2())
            playerInterface.requestDecision(state, YesNoDecision(player1Id, "Test"))

            playerInterface.remainingResponses shouldBe 1
        }

        test("reset allows reusing responses") {
            val playerInterface = ScriptedPlayerInterface.withResponses(YesNoChoice(true))
            val state = GameState.newGame(createPlayer1(), createPlayer2())
            val decision = YesNoDecision(player1Id, "Test")

            playerInterface.requestDecision(state, decision)
            playerInterface.remainingResponses shouldBe 0

            playerInterface.reset()
            playerInterface.remainingResponses shouldBe 1

            val response = playerInterface.requestDecision(state, decision)
            (response as YesNoChoice).choice shouldBe true
        }
    }

    context("AutoPlayerInterface") {
        test("returns default responses for each decision type") {
            val state = GameState.newGame(createPlayer1(), createPlayer2())
            val playerInterface = AutoPlayerInterface()

            // YesNoDecision defaults to false
            val yesNoResponse = playerInterface.requestDecision(
                state, YesNoDecision(player1Id, "Test?")
            )
            yesNoResponse.shouldBeInstanceOf<YesNoChoice>()
            (yesNoResponse as YesNoChoice).choice shouldBe false

            // ChooseNumber defaults to minimum
            val numberResponse = playerInterface.requestDecision(
                state, ChooseNumber(player1Id, "Choose", 5, 10)
            )
            numberResponse.shouldBeInstanceOf<NumberChoice>()
            (numberResponse as NumberChoice).number shouldBe 5

            // PriorityDecision defaults to Pass
            val priorityResponse = playerInterface.requestDecision(
                state, PriorityDecision(player1Id, true, true, true, true)
            )
            priorityResponse.shouldBeInstanceOf<PriorityChoice.Pass>()

            // MulliganDecision defaults to Keep
            val mulliganResponse = playerInterface.requestDecision(
                state, MulliganDecision(player1Id, 7, 0)
            )
            mulliganResponse.shouldBeInstanceOf<MulliganChoice.Keep>()
        }
    }

    context("MultiPlayerInterface") {
        test("routes decisions to correct player interface") {
            val player1Interface = ScriptedPlayerInterface.withResponses(YesNoChoice(true))
            val player2Interface = ScriptedPlayerInterface.withResponses(YesNoChoice(false))

            val multiInterface = MultiPlayerInterface(
                mapOf(
                    player1Id to player1Interface,
                    player2Id to player2Interface
                )
            )

            val state = GameState.newGame(createPlayer1(), createPlayer2())

            val response1 = multiInterface.requestDecision(
                state, YesNoDecision(player1Id, "Player 1 decision")
            )
            val response2 = multiInterface.requestDecision(
                state, YesNoDecision(player2Id, "Player 2 decision")
            )

            (response1 as YesNoChoice).choice shouldBe true
            (response2 as YesNoChoice).choice shouldBe false
        }
    }

    context("LoggingPlayerInterface") {
        test("logs all decisions") {
            val delegate = ScriptedPlayerInterface.withResponses(
                YesNoChoice(true),
                NumberChoice(7)
            )
            val loggingInterface = LoggingPlayerInterface(delegate) { _, _ -> }

            val state = GameState.newGame(createPlayer1(), createPlayer2())

            loggingInterface.requestDecision(state, YesNoDecision(player1Id, "First?"))
            loggingInterface.requestDecision(state, ChooseNumber(player1Id, "Number", 0, 10))

            loggingInterface.decisionLog.size shouldBe 2
            loggingInterface.decisionLog[0].first.shouldBeInstanceOf<YesNoDecision>()
            loggingInterface.decisionLog[0].second.shouldBeInstanceOf<YesNoChoice>()
            loggingInterface.decisionLog[1].first.shouldBeInstanceOf<ChooseNumber>()
            loggingInterface.decisionLog[1].second.shouldBeInstanceOf<NumberChoice>()
        }
    }
})
