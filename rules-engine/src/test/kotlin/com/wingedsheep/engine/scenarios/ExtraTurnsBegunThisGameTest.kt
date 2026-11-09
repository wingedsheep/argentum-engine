package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.engineSerializersModule
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.player.ExtraTurnsComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.OptionalSkipTurnWith
import com.wingedsheep.sdk.scripting.SkipExtraTurns
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json

/**
 * `DynamicAmount.ExtraTurnsBegunThisGame` — the game-long count of extra turns a player has
 * *begun* (CR 500.7 adds an extra turn directly after the specified turn). It is stamped when the
 * inserted turn actually starts, so ordinary turns, prevented extra turns, and an extra turn skipped
 * before it begins never count.
 */
class ExtraTurnsBegunThisGameTest : ScenarioTestBase() {
    init {
        val nexus = card("Extra Turn Preventer") {
            typeLine = "Artifact"
            replacementEffect(SkipExtraTurns())
        }
        val skipper = card("Extra Turn Skip Source") {
            typeLine = "Artifact"
            replacementEffect(OptionalSkipTurnWith(Effects.Untap(EffectTarget.Self),
                restrictions = listOf(Conditions.SourceIsTapped)))
        }
        cardRegistry.register(nexus)
        cardRegistry.register(skipper)

        fun board(configure: ScenarioBuilder.() -> ScenarioBuilder = { this }) = scenario()
            .withPlayers("Taker", "Opponent")
            .withCardInLibrary(1, "Forest").withCardInLibrary(1, "Forest")
            .withCardInLibrary(2, "Forest").withCardInLibrary(2, "Forest")
            .configure()
            .withActivePlayer(1).withPriorityPlayer(1).inPhase(Phase.ENDING, Step.END).build()

        fun TestGame.takeExtraTurn(taker: EntityId = player1Id) {
            val result = services.effectExecutorRegistry.execute(state, Effects.TakeExtraTurn(),
                EffectContext(sourceId = null, controllerId = taker))
            result.error shouldBe null
            state = result.state
        }
        fun TestGame.endTurn() {
            val result = services.turnManager.endTurn(state)
            result.error shouldBe null
            state = result.state
        }
        fun TestGame.begun(player: EntityId): Int =
            state.getEntity(player)?.get<ExtraTurnsComponent>()?.begun ?: 0
        fun TestGame.amount(player: EntityId): Int =
            services.dynamicAmountEvaluator.evaluate(state, DynamicAmounts.extraTurnsBegunThisGame(Player.You),
                EffectContext(sourceId = null, controllerId = player))

        test("beginning an extra turn counts for its taker only; ordinary turns never count") {
            val game = board()
            game.amount(game.player1Id) shouldBe 0

            game.takeExtraTurn()
            withClue("creating the extra turn doesn't count — it hasn't begun") {
                game.begun(game.player1Id) shouldBe 0
            }
            game.endTurn()
            game.state.activePlayerId shouldBe game.player1Id
            game.amount(game.player1Id) shouldBe 1
            game.amount(game.player2Id) shouldBe 0

            game.endTurn()
            withClue("the opponent's ordinary turn adds nothing") {
                game.state.activePlayerId shouldBe game.player2Id
                game.begun(game.player1Id) shouldBe 1
                game.begun(game.player2Id) shouldBe 0
            }
            game.endTurn()
            withClue("nor does the taker's next ordinary turn") {
                game.state.activePlayerId shouldBe game.player1Id
                game.begun(game.player1Id) shouldBe 1
            }

            game.takeExtraTurn()
            game.endTurn()
            game.state.activePlayerId shouldBe game.player1Id
            game.amount(game.player1Id) shouldBe 2
        }

        test("the count survives serialization") {
            val game = board()
            game.takeExtraTurn()
            game.endTurn()
            val codec = Json { serializersModule = engineSerializersModule; encodeDefaults = true; allowStructuredMapKeys = true }
            game.state = codec.decodeFromString<GameState>(codec.encodeToString(GameState.serializer(), game.state))
            game.amount(game.player1Id) shouldBe 1
        }

        test("a prevented extra turn never begins, so it doesn't count") {
            val game = board { withCardOnBattlefield(2, nexus.name) }
            game.takeExtraTurn()
            game.endTurn()
            game.state.activePlayerId shouldBe game.player2Id
            game.begun(game.player1Id) shouldBe 0
        }

        test("an extra turn whose start offers a skip counts only when the taker begins it") {
            for (begin in listOf(true, false)) {
                val game = board { withCardOnBattlefield(1, skipper.name, tapped = true) }
                game.takeExtraTurn()
                game.endTurn()
                val decision = game.state.pendingDecision as ChooseOptionDecision
                decision.playerId shouldBe game.player1Id
                val beginIndex = decision.options.lastIndex
                game.submitDecision(OptionChosenResponse(decision.id, if (begin) beginIndex else 0)).error shouldBe null
                withClue("begin=$begin") {
                    if (begin) {
                        game.state.activePlayerId shouldBe game.player1Id
                        game.begun(game.player1Id) shouldBe 1
                    } else {
                        game.state.activePlayerId shouldBe game.player2Id
                        game.begun(game.player1Id) shouldBe 0
                    }
                }
            }
        }

        test("a non-active taker's extra turn is their very next turn, and only it counts") {
            // The opponent takes an extra turn during the active player's turn (Nexus of Fate at
            // instant speed): the extra turn comes directly after this one (CR 500.7).
            val game = board()
            game.takeExtraTurn(game.player2Id)
            game.endTurn()
            withClue("the opponent's next turn is the extra turn") {
                game.state.activePlayerId shouldBe game.player2Id
                game.amount(game.player2Id) shouldBe 1
            }
            game.endTurn()
            withClue("their following turn is ordinary — still one begun") {
                game.state.activePlayerId shouldBe game.player2Id
                game.amount(game.player2Id) shouldBe 1
                game.amount(game.player1Id) shouldBe 0
            }
            game.endTurn()
            game.state.activePlayerId shouldBe game.player1Id
            game.amount(game.player2Id) shouldBe 1
        }

        test("an extra turn that is skipped never begins, so it doesn't count") {
            val game = board()
            game.takeExtraTurn()
            val result = services.effectExecutorRegistry.execute(game.state,
                Effects.SkipNextTurn(EffectTarget.PlayerRef(Player.You)),
                EffectContext(sourceId = null, controllerId = game.player1Id))
            result.error shouldBe null
            game.state = result.state
            game.endTurn()
            withClue("the extra turn is skipped, so the opponent goes next") {
                game.state.activePlayerId shouldBe game.player2Id
                game.amount(game.player1Id) shouldBe 0
            }
            game.endTurn()
            game.state.activePlayerId shouldBe game.player1Id
            withClue("the taker's next ordinary turn doesn't count either") { game.amount(game.player1Id) shouldBe 0 }
        }

        test("in multiplayer, only the taker's extra turn counts — not a bystander's ordinary turn") {
            val driver = com.wingedsheep.engine.support.GameTestDriver()
            driver.registerCards(com.wingedsheep.engine.support.TestCards.all)
            val deck = com.wingedsheep.sdk.model.Deck.of("Forest" to 40)
            val (a, b, c) = driver.initMultiplayer(listOf(deck, deck, deck), startingPlayer = 0)
            driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
            driver.activePlayer shouldBe a
            fun begun(p: EntityId) = driver.state.getEntity(p)?.get<ExtraTurnsComponent>()?.begun ?: 0
            fun endTurn() {
                val result = driver.services.turnManager.endTurn(driver.state)
                result.error shouldBe null
                driver.replaceState(result.state)
            }
            val r = driver.services.effectExecutorRegistry.execute(driver.state, Effects.TakeExtraTurn(),
                EffectContext(sourceId = null, controllerId = c))
            r.error shouldBe null
            driver.replaceState(r.state)

            endTurn()
            withClue("the third seat takes the extra turn right after the active player's") {
                driver.activePlayer shouldBe c
                begun(c) shouldBe 1
            }
            endTurn()
            endTurn()
            withClue("the seats' ordinary turns never count") {
                begun(a) shouldBe 0
                begun(b) shouldBe 0
                begun(c) shouldBe 1
            }
        }
    }
}
