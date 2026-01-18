package com.wingedsheep.rulesengine.game

import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.CardInstance
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype
import com.wingedsheep.rulesengine.player.Player
import com.wingedsheep.rulesengine.player.PlayerId
import com.wingedsheep.rulesengine.zone.ZoneType
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.assertions.throwables.shouldThrow

class GameStateTest : FunSpec({

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

    context("creation") {
        test("newGame creates initial state") {
            val state = GameState.newGame(createPlayer1(), createPlayer2())

            state.players.size shouldBe 2
            state.battlefield.isEmpty.shouldBeTrue()
            state.stack.isEmpty.shouldBeTrue()
            state.exile.isEmpty.shouldBeTrue()
            state.isGameOver.shouldBeFalse()
            state.winner.shouldBeNull()
        }

        test("newGame requires at least 2 players") {
            shouldThrow<IllegalArgumentException> {
                GameState.newGame(listOf(createPlayer1()))
            }
        }

        test("activePlayer is first player") {
            val state = GameState.newGame(createPlayer1(), createPlayer2())
            state.activePlayer.id shouldBe player1Id
        }
    }

    context("player access") {
        test("getPlayer returns correct player") {
            val state = GameState.newGame(createPlayer1(), createPlayer2())

            state.getPlayer(player1Id).name shouldBe "Alice"
            state.getPlayer(player2Id).name shouldBe "Bob"
        }

        test("getPlayer throws for unknown player") {
            val state = GameState.newGame(createPlayer1(), createPlayer2())
            val unknownId = PlayerId.of("unknown")

            shouldThrow<IllegalStateException> {
                state.getPlayer(unknownId)
            }
        }
    }

    context("player updates") {
        test("updatePlayer modifies specific player") {
            val state = GameState.newGame(createPlayer1(), createPlayer2())
            val updated = state.updatePlayer(player1Id) { it.gainLife(5) }

            updated.getPlayer(player1Id).life shouldBe 25
            updated.getPlayer(player2Id).life shouldBe 20
        }

        test("updateActivePlayer modifies active player") {
            val state = GameState.newGame(createPlayer1(), createPlayer2())
            val updated = state.updateActivePlayer { it.loseLife(3) }

            updated.activePlayer.life shouldBe 17
        }

        test("updateAllPlayers modifies all players") {
            val state = GameState.newGame(createPlayer1(), createPlayer2())
            val updated = state.updateAllPlayers { it.gainLife(10) }

            updated.getPlayer(player1Id).life shouldBe 30
            updated.getPlayer(player2Id).life shouldBe 30
        }
    }

    context("zone updates") {
        test("updateBattlefield modifies battlefield") {
            val state = GameState.newGame(createPlayer1(), createPlayer2())
            val card = CardInstance.create(bearDef, player1Id.value)

            val updated = state.updateBattlefield { it.addToTop(card) }

            updated.battlefield.size shouldBe 1
            updated.battlefield.contains(card.id).shouldBeTrue()
        }

        test("updateStack modifies stack") {
            val state = GameState.newGame(createPlayer1(), createPlayer2())
            val card = CardInstance.create(bearDef, player1Id.value)

            val updated = state.updateStack { it.addToTop(card) }

            updated.stack.size shouldBe 1
        }
    }

    context("turn progression") {
        test("advanceStep moves to next step") {
            val state = GameState.newGame(createPlayer1(), createPlayer2())
            val advanced = state.advanceStep()

            advanced.currentStep shouldBe Step.UPKEEP
        }

        test("advanceToPhase jumps to phase") {
            val state = GameState.newGame(createPlayer1(), createPlayer2())
            val combat = state.advanceToPhase(Phase.COMBAT)

            combat.currentPhase shouldBe Phase.COMBAT
            combat.currentStep shouldBe Step.BEGIN_COMBAT
        }
    }

    context("card finding") {
        test("findCard finds card on battlefield") {
            val state = GameState.newGame(createPlayer1(), createPlayer2())
            val card = CardInstance.create(bearDef, player1Id.value)
            val withCard = state.updateBattlefield { it.addToTop(card) }

            val location = withCard.findCard(card.id)

            location.shouldNotBeNull()
            location.zone shouldBe ZoneType.BATTLEFIELD
            location.card shouldBe card
        }

        test("findCard finds card in player hand") {
            var state = GameState.newGame(createPlayer1(), createPlayer2())
            val card = CardInstance.create(bearDef, player1Id.value)
            state = state.updatePlayer(player1Id) { p ->
                p.updateHand { it.addToTop(card) }
            }

            val location = state.findCard(card.id)

            location.shouldNotBeNull()
            location.zone shouldBe ZoneType.HAND
            location.owner shouldBe player1Id
        }

        test("findCard returns null for unknown card") {
            val state = GameState.newGame(createPlayer1(), createPlayer2())
            val card = CardInstance.create(bearDef, player1Id.value)

            state.findCard(card.id).shouldBeNull()
        }
    }

    context("battlefield queries") {
        test("getCardsOnBattlefield returns all cards") {
            val state = GameState.newGame(createPlayer1(), createPlayer2())
            val card1 = CardInstance.create(bearDef, player1Id.value)
            val card2 = CardInstance.create(bearDef, player2Id.value)

            val withCards = state
                .updateBattlefield { it.addToTop(card1) }
                .updateBattlefield { it.addToTop(card2) }

            withCards.getCardsOnBattlefield().size shouldBe 2
        }

        test("getCreaturesControlledBy filters by controller") {
            val state = GameState.newGame(createPlayer1(), createPlayer2())
            val card1 = CardInstance.create(bearDef, player1Id.value)
            val card2 = CardInstance.create(bearDef, player2Id.value)

            val withCards = state
                .updateBattlefield { it.addToTop(card1) }
                .updateBattlefield { it.addToTop(card2) }

            withCards.getCreaturesControlledBy(player1Id).size shouldBe 1
            withCards.getCreaturesControlledBy(player2Id).size shouldBe 1
        }
    }

    context("game ending") {
        test("endGame with winner") {
            val state = GameState.newGame(createPlayer1(), createPlayer2())
            val ended = state.endGame(player1Id)

            ended.isGameOver.shouldBeTrue()
            ended.winner shouldBe player1Id
        }

        test("endGame with no winner (draw)") {
            val state = GameState.newGame(createPlayer1(), createPlayer2())
            val ended = state.endGame(null)

            ended.isGameOver.shouldBeTrue()
            ended.winner.shouldBeNull()
        }
    }

    context("state properties") {
        test("isMainPhase") {
            val state = GameState.newGame(createPlayer1(), createPlayer2())
                .advanceToStep(Step.PRECOMBAT_MAIN)

            state.isMainPhase.shouldBeTrue()
        }

        test("stackIsEmpty") {
            val state = GameState.newGame(createPlayer1(), createPlayer2())
            state.stackIsEmpty.shouldBeTrue()

            val card = CardInstance.create(bearDef, player1Id.value)
            val withStack = state.updateStack { it.addToTop(card) }
            withStack.stackIsEmpty.shouldBeFalse()
        }
    }
})
