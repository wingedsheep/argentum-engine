package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.PhasedOutComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.player.SkipNextTurnComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json

class TurnStartReplacementTest : ScenarioTestBase() {
    init {
        fun TestGame.isTapped(id: com.wingedsheep.sdk.model.EntityId) =
            state.getEntity(id)?.has<com.wingedsheep.engine.state.components.battlefield.TappedComponent>() == true
        val source = card("Turn Skip Test Source") {
            typeLine = "Artifact"
            replacementEffect(OptionalSkipTurnWith(Effects.Untap(EffectTarget.Self), restrictions = listOf(Conditions.SourceIsTapped)))
        }
        val opponent = card("Turn Skip Test Opponent") {
            typeLine = "Artifact"
            replacementEffect(OptionalSkipTurnWith(Effects.GainLife(3), EventPattern.TurnBeginEvent(Player.EachOpponent)))
        }
        val draw = card("Turn Skip Test Draw") {
            typeLine = "Artifact"
            replacementEffect(OptionalSkipTurnWith(Effects.May(Effects.DrawCards(1)).then(Effects.GainLife(2)),
                restrictions = listOf(Conditions.SourceIsTapped)))
        }
        val identity = card("Turn Skip Test Identity") {
            typeLine = "Artifact"
            replacementEffect(OptionalSkipTurnWith(Effects.May(Effects.GainLife(1)).then(Effects.Untap(EffectTarget.Self)),
                restrictions = listOf(Conditions.SourceIsTapped)))
        }
        listOf(source, opponent, draw, identity).forEach(cardRegistry::register)
        fun board(name: String = source.name, copies: Int = 1, tapped: Boolean = true) = scenario()
            .withPlayers("Owner", "Opponent")
            .apply { repeat(copies) { withCardOnBattlefield(1, name, tapped = tapped) } }
            .withCardInLibrary(1, "Forest").withCardInLibrary(1, "Forest").withCardInLibrary(2, "Forest")
            .withActivePlayer(2).withPriorityPlayer(2).inPhase(Phase.ENDING, Step.END).build()
        fun end(game: TestGame): ExecutionResult = services.turnManager.endTurn(game.state).also {
            it.error shouldBe null; game.state = it.state
        }
        fun choose(game: TestGame, index: Int = 0): ExecutionResult {
            val decision = game.state.pendingDecision as ChooseOptionDecision
            return game.submitDecision(OptionChosenResponse(decision.id, index)).also { it.error shouldBe null }
        }
        fun effect(game: TestGame, effect: Effect, id: com.wingedsheep.sdk.model.EntityId) {
            val result = services.effectExecutorRegistry.execute(game.state, effect,
                EffectContext(sourceId = id, controllerId = game.player1Id))
            result.error shouldBe null; game.state = result.state
        }
        test("declining begins exactly one turn and creates no follow-up") {
            val game = board(); val n = game.state.turnNumber
            end(game).outcome.shouldBe(Outcome.Paused(game.state.pendingDecision!!))
            game.state.turnNumber shouldBe n
            game.state.priorityPlayerId shouldBe null
            (game.state.pendingDecision as ChooseOptionDecision).defaultSearch shouldBe "Begin this turn"
            choose(game, 1)
            game.state.activePlayerId shouldBe game.player1Id
            game.state.turnNumber shouldBe n + 1
            game.state.step shouldBe Step.UPKEEP
        }
        test("accepting skips without taking a turn number and untaps before next untap actions") {
            val game = board(); val id = game.findPermanent(source.name)!!; val n = game.state.turnNumber
            end(game); val result = choose(game)
            result.events.filterIsInstance<TurnSkippedEvent>().single().playerId shouldBe game.player1Id
            result.events.filterIsInstance<UntappedEvent>().any { it.entityId == id } shouldBe true
            game.state.activePlayerId shouldBe game.player2Id
            game.state.turnNumber shouldBe n + 1
            game.isTapped(id) shouldBe false
            game.state.continuationStack shouldBe emptyList()
        }
        test("multiple sources offer one selected replacement and leave the other tapped") {
            val game = board(copies = 2); end(game)
            val decision = game.state.pendingDecision as ChooseOptionDecision
            decision.options.size shouldBe 3
            val chosenId = decision.optionCardIds!![1]!!.single()
            choose(game, 1)
            game.isTapped(chosenId) shouldBe false
            game.state.getBattlefield().count { game.isTapped(it) } shouldBe 1
        }
        test("optional skip preserves a pending mandatory skip for a later occurrence") {
            val game = board()
            game.state = game.state.updateEntity(game.player1Id) { it.with(SkipNextTurnComponent()) }
            end(game); choose(game)
            game.state.getEntity(game.player1Id)!!.get<SkipNextTurnComponent>()!!.turns shouldBe 1
            val result = end(game)
            result.events.filterIsInstance<TurnSkippedEvent>().size shouldBe 1
            game.state.activePlayerId shouldBe game.player2Id
            game.state.getEntity(game.player1Id)!!.has<SkipNextTurnComponent>() shouldBe false
        }
        test("mandatory skip chosen instead leaves source tapped and consumes only that skip") {
            val game = board()
            game.state = game.state.updateEntity(game.player1Id) { it.with(SkipNextTurnComponent(2)) }
            end(game); choose(game, 1)
            game.isTapped(game.findPermanent(source.name)!!) shouldBe true
            game.state.getEntity(game.player1Id)!!.get<SkipNextTurnComponent>()!!.turns shouldBe 1
        }
        test("follow-up waits across a second skip and executes in the next actual turn") {
            val game = board()
            game.state = game.state.updateEntity(game.player2Id) { it.with(SkipNextTurnComponent()) }
            val id = game.findPermanent(source.name)!!
            end(game); choose(game)
            (game.state.pendingDecision as ChooseOptionDecision).playerId shouldBe game.player1Id
            game.isTapped(id) shouldBe true
            choose(game, 1)
            game.state.activePlayerId shouldBe game.player1Id
            game.isTapped(id) shouldBe false
        }
        test("source condition untapped face down and phased out suppress printed replacement") {
            for (kind in listOf("untapped", "face down", "phased out")) {
                val game = board(tapped = kind != "untapped"); val id = game.findPermanent(source.name)!!
                if (kind == "face down") game.state = game.state.updateEntity(id) { it.with(FaceDownComponent) }
                if (kind == "phased out") game.state = game.state.updateEntity(id) { it.with(PhasedOutComponent(game.player1Id)) }
                end(game)
                game.state.pendingDecision shouldBe null
                game.state.activePlayerId shouldBe game.player1Id
            }
        }
        test("lost abilities suppress printed replacement") {
            val game = board(); val id = game.findPermanent(source.name)!!
            effect(game, Effects.RemoveAllAbilities(EffectTarget.Self, Duration.Permanent), id)
            end(game); game.state.pendingDecision shouldBe null
        }
        test("projected control determines whose turn can be skipped") {
            val game = board(); val id = game.findPermanent(source.name)!!
            val result = services.effectExecutorRegistry.execute(game.state, Effects.GainControl(EffectTarget.Self),
                EffectContext(sourceId = id, controllerId = game.player2Id))
            game.state = result.state
            end(game); game.state.pendingDecision shouldBe null
            end(game); (game.state.pendingDecision as ChooseOptionDecision).playerId shouldBe game.player2Id
        }
        test("affected opponent decides while follow-up You names the source controller") {
            val game = board(opponent.name)
            // The owner's turn is unaffected, then the opponent chooses its own skipped occurrence.
            end(game); game.state.pendingDecision shouldBe null
            end(game); (game.state.pendingDecision as ChooseOptionDecision).playerId shouldBe game.player2Id
            choose(game); game.getLifeTotal(1) shouldBe 23
            game.getLifeTotal(2) shouldBe 20
        }
        test("paused turn replacement survives state serialization") {
            val game = board(); end(game)
            val codec = Json { serializersModule = engineSerializersModule; encodeDefaults = true; allowStructuredMapKeys = true }
            game.state = codec.decodeFromString<GameState>(codec.encodeToString(GameState.serializer(), game.state))
            choose(game); game.state.activePlayerId shouldBe game.player2Id
            game.state.pendingDecision shouldBe null
        }
        test("follow-up decision pauses before untap and resumes the composed tail and upkeep") {
            val game = board(draw.name); end(game); choose(game)
            game.state.step shouldBe Step.UNTAP
            game.state.priorityPlayerId shouldBe null
            game.getLifeTotal(1) shouldBe 20
            val decision = game.state.pendingDecision as YesNoDecision
            val codec = Json { serializersModule = engineSerializersModule; encodeDefaults = true; allowStructuredMapKeys = true }
            game.state = codec.decodeFromString<GameState>(codec.encodeToString(GameState.serializer(), game.state))
            game.submitDecision(YesNoResponse(decision.id, true)).error shouldBe null
            game.handSize(1) shouldBe 1
            game.getLifeTotal(1) shouldBe 22
            game.state.step shouldBe Step.UPKEEP
            game.state.activePlayerId shouldBe game.player2Id
            game.state.continuationStack shouldBe emptyList()
        }
        test("a paused follow-up cannot untap a new battlefield visit of its source") {
            val game = board(identity.name); val id = game.findPermanent(identity.name)!!
            end(game); choose(game)
            val decision = game.state.pendingDecision as YesNoDecision
            game.state = services.zones.moveToZone(game.state, id, Zone.HAND).state
            game.state = services.zones.moveToZone(game.state, id, Zone.BATTLEFIELD).state
                .updateEntity(id) { it.with(com.wingedsheep.engine.state.components.battlefield.TappedComponent) }
            game.submitDecision(YesNoResponse(decision.id, true)).error shouldBe null
            game.isTapped(id) shouldBe true
            game.state.step shouldBe Step.UPKEEP
        }
        test("a long chain of consecutive skipped occurrences consumes counts without starting phantom turns") {
            val game = board(tapped = false); val n = game.state.turnNumber
            game.state = game.state.updateEntity(game.player1Id) { it.with(SkipNextTurnComponent(1000)) }
                .updateEntity(game.player2Id) { it.with(SkipNextTurnComponent(1000)) }
            val result = end(game)
            result.events.filterIsInstance<TurnSkippedEvent>().size shouldBe 2000
            game.state.turnNumber shouldBe n + 1
            game.state.activePlayerId shouldBe game.player1Id
            game.state.getEntity(game.player1Id)!!.has<SkipNextTurnComponent>() shouldBe false
            game.state.getEntity(game.player2Id)!!.has<SkipNextTurnComponent>() shouldBe false
        }
        test("a grant on a face-down permanent does not expose its underlying name to the opponent") {
            val game = board(tapped = false); val id = game.findPermanent(source.name)!!
            effect(game, Effects.GrantReplacementEffect(OptionalSkipTurnWith(Effects.GainLife(1),
                EventPattern.TurnBeginEvent(Player.EachOpponent)), EffectTarget.Self, Duration.Permanent), id)
            game.state = game.state.updateEntity(id) { it.with(FaceDownComponent) }
            end(game); end(game)
            val decision = game.state.pendingDecision as ChooseOptionDecision
            decision.playerId shouldBe game.player2Id
            decision.options.first().contains(source.name) shouldBe false
            decision.options.first().contains("face-down permanent") shouldBe true
        }
        test("granted replacements use captured controller and expire with their duration") {
            val game = board(tapped = false); val id = game.findPermanent(source.name)!!
            effect(game, Effects.GrantReplacementEffect(OptionalSkipTurnWith(Effects.GainLife(4)),
                EffectTarget.Self, Duration.Permanent), id)
            val stolen = services.effectExecutorRegistry.execute(game.state, Effects.GainControl(EffectTarget.Self),
                EffectContext(sourceId = id, controllerId = game.player2Id))
            stolen.error shouldBe null; game.state = stolen.state
            end(game); (game.state.pendingDecision as ChooseOptionDecision).playerId shouldBe game.player1Id
            choose(game); game.getLifeTotal(1) shouldBe 24
            val expiring = board(tapped = false); val other = expiring.findPermanent(source.name)!!
            effect(expiring, Effects.GrantReplacementEffect(OptionalSkipTurnWith(Effects.GainLife(4)),
                EffectTarget.Self, Duration.EndOfTurn), other)
            end(expiring); expiring.state.pendingDecision shouldBe null
        }
        test("departed seat durations expire when skipped turns walk a complete circuit") {
            val game = board(tapped = false)
            val id = game.findPermanent(source.name)!!
            val departed = com.wingedsheep.sdk.model.EntityId.of("departed-seat")
            val fourth = com.wingedsheep.sdk.model.EntityId.of("fourth-seat")
            game.state = game.state
                .withEntity(departed, game.state.getEntity(game.player1Id)!!.with(
                    com.wingedsheep.engine.state.components.player.PlayerLostComponent(
                        com.wingedsheep.engine.state.components.player.LossReason.CONCESSION)))
                .withEntity(fourth, game.state.getEntity(game.player1Id)!!)
                .copy(turnOrder = listOf(game.player2Id, game.player1Id, departed, fourth))
            val suppressed = services.effectExecutorRegistry.execute(game.state,
                Effects.RemoveAllAbilities(EffectTarget.SpecificEntity(id), Duration.UntilYourNextTurn),
                EffectContext(sourceId = id, controllerId = departed))
            suppressed.error shouldBe null
            game.state = suppressed.state
            game.state.projectedState.hasLostAllAbilities(id) shouldBe true
            for (player in listOf(game.player1Id, fourth, game.player2Id)) {
                game.state = game.state.updateEntity(player) { it.with(SkipNextTurnComponent()) }
            }
            val result = end(game)
            result.events.filterIsInstance<TurnSkippedEvent>().map { it.playerId } shouldBe
                listOf(game.player1Id, fourth, game.player2Id)
            game.state.activePlayerId shouldBe game.player1Id
            game.state.projectedState.hasLostAllAbilities(id) shouldBe false
            game.state.floatingEffects.any { it.controllerId == departed } shouldBe false
        }
        test("departed seat ability suppression expires before the next turn replacement choice") {
            val game = board()
            val id = game.findPermanent(source.name)!!
            val departed = com.wingedsheep.sdk.model.EntityId.of("departed-seat")
            game.state = game.state
                .withEntity(departed, game.state.getEntity(game.player1Id)!!.with(
                    com.wingedsheep.engine.state.components.player.PlayerLostComponent(
                        com.wingedsheep.engine.state.components.player.LossReason.CONCESSION)))
                .copy(turnOrder = listOf(game.player2Id, departed, game.player1Id))
            val suppressed = services.effectExecutorRegistry.execute(game.state,
                Effects.RemoveAllAbilities(EffectTarget.SpecificEntity(id), Duration.UntilYourNextTurn),
                EffectContext(sourceId = id, controllerId = departed))
            suppressed.error shouldBe null
            game.state = suppressed.state
            game.state.projectedState.hasLostAllAbilities(id) shouldBe true
            end(game)
            (game.state.pendingDecision as ChooseOptionDecision).playerId shouldBe game.player1Id
            game.state.projectedState.hasLostAllAbilities(id) shouldBe false
            choose(game)
            game.state.activePlayerId shouldBe game.player2Id
            game.isTapped(id) shouldBe false
        }
        test("an inserted extra turn does not expire a departed seat duration before its ordinary turn") {
            val game = board(tapped = false)
            val id = game.findPermanent(source.name)!!
            val departed = com.wingedsheep.sdk.model.EntityId.of("departed-seat")
            game.state = game.state
                .withEntity(departed, game.state.getEntity(game.player1Id)!!.with(
                    com.wingedsheep.engine.state.components.player.PlayerLostComponent(
                        com.wingedsheep.engine.state.components.player.LossReason.CONCESSION)))
                .copy(turnOrder = listOf(game.player2Id, departed, game.player1Id))
            val suppressed = services.effectExecutorRegistry.execute(game.state,
                Effects.RemoveAllAbilities(EffectTarget.SpecificEntity(id), Duration.UntilYourNextTurn),
                EffectContext(sourceId = id, controllerId = departed))
            suppressed.error shouldBe null
            game.state = suppressed.state
            val extra = services.effectExecutorRegistry.execute(game.state, Effects.TakeExtraTurn(),
                EffectContext(sourceId = id, controllerId = game.player2Id))
            extra.error shouldBe null
            game.state = extra.state
            end(game)
            game.state.activePlayerId shouldBe game.player2Id
            game.state.projectedState.hasLostAllAbilities(id) shouldBe true
            game.state.floatingEffects.any { it.controllerId == departed } shouldBe true
            end(game)
            game.state.activePlayerId shouldBe game.player1Id
            game.state.projectedState.hasLostAllAbilities(id) shouldBe false
            game.state.floatingEffects.any { it.controllerId == departed } shouldBe false
        }
    }
}
