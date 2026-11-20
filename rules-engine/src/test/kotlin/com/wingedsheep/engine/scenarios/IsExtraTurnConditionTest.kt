package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.engineSerializersModule
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.SkipExtraTurns
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json

/**
 * `Conditions.IsExtraTurn` — "during extra turns" (CR 500.7). True for the whole of a turn that
 * began as an extra turn, whoever's turn it is, and false during every natural turn; an extra
 * turn that is skipped or prevented never begins, so the turn that begins instead is natural.
 */
class IsExtraTurnConditionTest : ScenarioTestBase() {
    init {
        val nexus = card("Extra Turn Preventer (IsExtraTurn)") {
            typeLine = "Artifact"
            replacementEffect(SkipExtraTurns())
        }
        cardRegistry.register(nexus)

        fun board(configure: ScenarioBuilder.() -> ScenarioBuilder = { this }) = scenario()
            .withPlayers("Taker", "Opponent")
            .withCardInLibrary(1, "Forest").withCardInLibrary(1, "Forest").withCardInLibrary(1, "Forest")
            .withCardInLibrary(2, "Forest").withCardInLibrary(2, "Forest").withCardInLibrary(2, "Forest")
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
        fun TestGame.isExtra(viewer: EntityId = player1Id): Boolean =
            services.predicateEvaluator.conditions.evaluate(state, Conditions.IsExtraTurn,
                EffectContext(sourceId = null, controllerId = viewer))

        test("the extra turn reads true for every player; the turns around it read false") {
            val game = board()
            withClue("a natural turn") { game.isExtra() shouldBe false }
            game.takeExtraTurn()
            withClue("creating the extra turn doesn't make the current turn one") { game.isExtra() shouldBe false }

            game.endTurn()
            game.state.activePlayerId shouldBe game.player1Id
            withClue("the extra turn, from either seat") {
                game.isExtra(game.player1Id) shouldBe true
                game.isExtra(game.player2Id) shouldBe true
            }
            game.endTurn()
            withClue("the opponent's natural turn") {
                game.state.activePlayerId shouldBe game.player2Id
                game.isExtra() shouldBe false
            }
            game.endTurn()
            withClue("the taker's next natural turn") {
                game.state.activePlayerId shouldBe game.player1Id
                game.isExtra() shouldBe false
            }
        }

        test("two extra turns in a row both read true, and the natural turn after them false") {
            val game = board()
            game.takeExtraTurn()
            game.takeExtraTurn()
            game.endTurn()
            game.state.activePlayerId shouldBe game.player1Id
            game.isExtra() shouldBe true
            game.endTurn()
            game.state.activePlayerId shouldBe game.player1Id
            game.isExtra() shouldBe true
            game.endTurn()
            game.state.activePlayerId shouldBe game.player2Id
            game.isExtra() shouldBe false
        }

        test("a non-active taker's extra turn is their next turn; their following turn is natural") {
            val game = board()
            game.takeExtraTurn(game.player2Id)
            game.endTurn()
            game.state.activePlayerId shouldBe game.player2Id
            game.isExtra() shouldBe true
            game.endTurn()
            game.state.activePlayerId shouldBe game.player2Id
            game.isExtra() shouldBe false
        }

        test("a prevented extra turn never begins, so the opponent's turn after it is natural") {
            val game = board { withCardOnBattlefield(2, nexus.name) }
            game.takeExtraTurn()
            game.endTurn()
            game.state.activePlayerId shouldBe game.player2Id
            game.isExtra() shouldBe false
        }

        test("a skipped extra turn never begins, so the turn that begins instead is natural") {
            val game = board()
            game.takeExtraTurn()
            val result = services.effectExecutorRegistry.execute(game.state,
                Effects.SkipNextTurn(EffectTarget.PlayerRef(Player.You)),
                EffectContext(sourceId = null, controllerId = game.player1Id))
            result.error shouldBe null
            game.state = result.state
            game.endTurn()
            game.state.activePlayerId shouldBe game.player2Id
            game.isExtra() shouldBe false
        }

        test("the flag survives serialization") {
            val game = board()
            game.takeExtraTurn()
            game.endTurn()
            val codec = Json { serializersModule = engineSerializersModule; encodeDefaults = true; allowStructuredMapKeys = true }
            game.state = codec.decodeFromString<GameState>(codec.encodeToString(GameState.serializer(), game.state))
            game.isExtra() shouldBe true
        }
    }
}
