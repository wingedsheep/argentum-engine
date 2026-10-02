package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.player.SkipNextTurnComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json

class ExtraTurnReplacementTest : ScenarioTestBase() {
    init {
        val source = card("Extra Turn Replacement Source") {
            typeLine = "Artifact"
            replacementEffect(OptionalSkipTurnWith(Effects.Untap(EffectTarget.Self),
                restrictions = listOf(Conditions.SourceIsTapped)))
        }
        cardRegistry.register(source)
        fun board() = scenario().withPlayers("Active", "Vault Owner")
            .withCardOnBattlefield(2, source.name, tapped = true)
            .withCardInLibrary(1, "Forest").withCardInLibrary(2, "Forest")
            .withActivePlayer(1).withPriorityPlayer(1).inPhase(Phase.ENDING, Step.END).build()
        fun extra(game: TestGame) {
            val result = services.effectExecutorRegistry.execute(game.state, Effects.TakeExtraTurn(),
                EffectContext(sourceId = game.findPermanent(source.name), controllerId = game.player1Id))
            result.error shouldBe null
            game.state = result.state
        }
        fun end(game: TestGame): ExecutionResult = services.turnManager.endTurn(game.state).also {
            it.error shouldBe null
            game.state = it.state
        }
        test("opponent cannot use a turn replacement while an extra turn is being inserted") {
            val game = board(); val id = game.findPermanent(source.name)!!
            extra(game)
            val badges = com.wingedsheep.engine.view.projection.PlayerActiveEffectsProjector(
                services.zones.predicateEvaluator).project(game.state, game.player2Id,
                game.state.getEntity(game.player2Id))
            badges.any { it.effectId == "skip_next_turn" } shouldBe false
            val codec = Json { serializersModule = engineSerializersModule; encodeDefaults = true; allowStructuredMapKeys = true }
            game.state = codec.decodeFromString<GameState>(codec.encodeToString(GameState.serializer(), game.state))
            val result = end(game)
            game.state.activePlayerId shouldBe game.player1Id
            game.state.pendingDecision shouldBe null
            game.state.getEntity(id)!!.has<TappedComponent>() shouldBe true
            result.events.filterIsInstance<TurnSkippedEvent>() shouldBe emptyList()
            end(game)
            (game.state.pendingDecision as ChooseOptionDecision).playerId shouldBe game.player2Id
        }
        test("a zone replacement extra turn bypasses opponent turn replacements too") {
            val game = board()
            val result = com.wingedsheep.engine.handlers.effects.ZoneMovementUtils.applyReplacementAdditionalEffect(
                services.zones, game.state, Effects.TakeExtraTurn(), game.player1Id)
            game.state = result.first
            end(game)
            game.state.activePlayerId shouldBe game.player1Id
            game.state.pendingDecision shouldBe null
            end(game)
            (game.state.pendingDecision as ChooseOptionDecision).playerId shouldBe game.player2Id
        }
        test("inserting an extra turn preserves a real pending skip added before or after it") {
            for (skipFirst in listOf(true, false)) {
                val game = board()
                fun skip() {
                    val result = services.effectExecutorRegistry.execute(game.state,
                        Effects.SkipNextTurn(EffectTarget.PlayerRef(com.wingedsheep.sdk.scripting.references.Player.AnOpponent)),
                        EffectContext(sourceId = game.findPermanent(source.name), controllerId = game.player1Id))
                    result.error shouldBe null; game.state = result.state
                }
                if (skipFirst) skip()
                extra(game)
                if (!skipFirst) skip()
                end(game)
                val remaining = game.state.getEntity(game.player2Id)!!.get<SkipNextTurnComponent>()!!
                remaining.turns shouldBe 1
                remaining.extraTurnBypasses shouldBe 0
                end(game)
                val decision = game.state.pendingDecision as ChooseOptionDecision
                game.submitDecision(OptionChosenResponse(decision.id, decision.options.lastIndex)).error shouldBe null
                game.state.activePlayerId shouldBe game.player1Id
                game.state.getEntity(game.player2Id)!!.has<SkipNextTurnComponent>() shouldBe false
            }
        }
    }
}
