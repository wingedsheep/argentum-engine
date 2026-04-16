package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.evaluation.*
import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.mtg.sets.definitions.portal.PortalSet
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

class AIPlayerTest : FunSpec({

    fun createCardRegistry(): CardRegistry {
        val registry = CardRegistry()
        registry.register(PortalSet.allCards)
        return registry
    }

    fun initGame(registry: CardRegistry, deck: Deck): Pair<GameState, ActionProcessor> {
        val initializer = GameInitializer(registry)
        val result = initializer.initializeGame(
            GameConfig(
                players = listOf(
                    PlayerConfig("AI Player", deck),
                    PlayerConfig("Opponent", deck)
                ),
                skipMulligans = true,
                startingPlayerIndex = 0
            )
        )
        return Pair(result.state, ActionProcessor(registry))
    }

    test("AI can evaluate board state") {
        val registry = createCardRegistry()
        val (state, _) = initGame(registry, Deck.of("Mountain" to 17, "Raging Goblin" to 3))

        val evaluator = AIPlayer.defaultEvaluator()
        val playerId = state.turnOrder[0]
        val score = evaluator.evaluate(state, state.projectedState, playerId)
        score.isFinite().shouldBeTrue()
    }

    test("AI can choose an action from legal actions") {
        val registry = createCardRegistry()
        val (state, _) = initGame(registry, Deck.of("Mountain" to 17, "Raging Goblin" to 3))

        val ai = AIPlayer.create(registry, state.turnOrder[0])
        val action = ai.chooseAction(state)
        action.shouldNotBeNull()
    }

    test("AI plays lands and creatures over multiple turns") {
        val registry = createCardRegistry()
        val deck = Deck.of("Mountain" to 14, "Raging Goblin" to 3, "Hill Giant" to 3)
        val (initialState, processor) = initGame(registry, deck)

        val p1 = initialState.turnOrder[0]
        val p2 = initialState.turnOrder[1]
        val ai1 = AIPlayer.create(registry, p1)
        val ai2 = AIPlayer.create(registry, p2)

        // Run the game for a few turns
        var state: GameState = initialState
        var safety = 0
        while (state.turnNumber < 3 && !state.gameOver && safety < 200) {
            val nextState: GameState? = when (state.priorityPlayerId) {
                p1 -> ai1.playPriorityWindow(state, processor)
                p2 -> ai2.playPriorityWindow(state, processor)
                else -> {
                    val d = state.pendingDecision
                    if (d != null) {
                        val ai = if (d.playerId == p1) ai1 else ai2
                        val r = processor.process(state, SubmitDecision(d.playerId, ai.respondToDecision(state, d))).result
                        if (r.error != null) null else r.state
                    } else null
                }
            }
            if (nextState == null) break
            state = nextState
            safety++
        }

        // After a few turns, both players should have permanents on the battlefield
        state.turnNumber shouldBeGreaterThan 0
        state.getBattlefield().size shouldBeGreaterThan 0
    }

    test("AI plays a full priority window without errors") {
        val registry = createCardRegistry()
        val (state, processor) = initGame(registry, Deck.of("Mountain" to 17, "Raging Goblin" to 3))

        val ai = AIPlayer.create(registry, state.turnOrder[0])
        val finalState = ai.playPriorityWindow(state, processor)
        finalState.shouldNotBeNull()
        // Just verify the AI didn't crash — it may not be in main phase yet
    }

    test("simulator produces valid results") {
        val registry = createCardRegistry()
        val (state, _) = initGame(registry, Deck.of("Mountain" to 17, "Raging Goblin" to 3))

        val simulator = GameSimulator(registry)
        val actions = simulator.getLegalActions(state, state.turnOrder[0])
        actions.size shouldBeGreaterThan 0

        for (action in actions.filter { it.affordable }) {
            val result = simulator.simulate(state, action.action)
            when (result) {
                is SimulationResult.Terminal -> result.state.shouldNotBeNull()
                is SimulationResult.NeedsDecision -> result.decision.shouldNotBeNull()
                is SimulationResult.Illegal -> {}
            }
        }
    }

    test("two AI players can play a full game") {
        val registry = createCardRegistry()
        val deck = Deck.of("Mountain" to 14, "Raging Goblin" to 3, "Hill Giant" to 3)
        val (initialState, processor) = initGame(registry, deck)

        val p1 = initialState.turnOrder[0]
        val p2 = initialState.turnOrder[1]
        val ai1 = AIPlayer.create(registry, p1)
        val ai2 = AIPlayer.create(registry, p2)

        var state: GameState = initialState
        var turns = 0
        val maxTurns = 50

        while (!state.gameOver && turns < maxTurns) {
            val nextState: GameState? = when (state.priorityPlayerId) {
                p1 -> ai1.playPriorityWindow(state, processor)
                p2 -> ai2.playPriorityWindow(state, processor)
                else -> {
                    val decision = state.pendingDecision
                    if (decision != null) {
                        val ai = if (decision.playerId == p1) ai1 else ai2
                        val response = ai.respondToDecision(state, decision)
                        val result = processor.process(state, SubmitDecision(decision.playerId, response)).result
                        if (result.error != null) null else result.state
                    } else null
                }
            }
            if (nextState == null) break
            state = nextState

            if (state.turnNumber > turns) {
                turns = state.turnNumber
            }
        }

        turns shouldBeGreaterThan 0

        val p1Life = state.getEntity(p1)?.get<LifeTotalComponent>()?.life ?: 20
        val p2Life = state.getEntity(p2)?.get<LifeTotalComponent>()?.life ?: 20
        (p1Life < 20 || p2Life < 20 || state.gameOver).shouldBeTrue()
    }

    test("searcher evaluates deeper than 1-ply without errors") {
        val registry = createCardRegistry()
        // Deck with instants — opponent can respond, triggering deeper search
        val deck = Deck.of("Mountain" to 12, "Raging Goblin" to 4, "Volcanic Hammer" to 4)
        val (state, _) = initGame(registry, deck)

        val playerId = state.turnOrder[0]
        val simulator = GameSimulator(registry)
        val evaluator = AIPlayer.defaultEvaluator()
        val searcher = Searcher(simulator, evaluator)

        val actions = simulator.getLegalActions(state, playerId).filter { it.affordable }
        // Search each action at depth 2 — should not throw
        for (action in actions.take(5)) {
            val score = searcher.searchAction(state, action, playerId, depth = 2)
            score.isFinite().shouldBeTrue()
        }
    }

    test("searcher at depth 2+ respects node limit") {
        val registry = createCardRegistry()
        val deck = Deck.of("Mountain" to 12, "Raging Goblin" to 4, "Volcanic Hammer" to 4)
        val (state, _) = initGame(registry, deck)

        val playerId = state.turnOrder[0]
        val simulator = GameSimulator(registry)
        val evaluator = AIPlayer.defaultEvaluator()
        // Very tight node limit
        val searcher = Searcher(simulator, evaluator, SearchConfig(maxDepth = 3, maxNodes = 50))

        val actions = simulator.getLegalActions(state, playerId).filter { it.affordable }
        for (action in actions.take(3)) {
            val score = searcher.searchAction(state, action, playerId, depth = 3)
            score.isFinite().shouldBeTrue()
        }
    }

    test("simulating CastSpell resolves the spell onto the battlefield") {
        val registry = createCardRegistry()
        val deck = Deck.of("Mountain" to 17, "Raging Goblin" to 3)
        val (initialState, processor) = initGame(registry, deck)

        val p1 = initialState.turnOrder[0]
        val ai1 = AIPlayer.create(registry, p1)
        val ai2 = AIPlayer.create(registry, initialState.turnOrder[1])

        // Advance to main phase
        var state: GameState = initialState
        var safety = 0
        while (state.phase != Phase.PRECOMBAT_MAIN && safety < 50 && !state.gameOver) {
            val nextState: GameState? = when (state.priorityPlayerId) {
                p1 -> ai1.playPriorityWindow(state, processor)
                else -> ai2.playPriorityWindow(state, processor)
            }
            if (nextState == null) break
            state = nextState
            safety++
        }

        // Now in main phase — play a land first
        if (state.phase == Phase.PRECOMBAT_MAIN && state.priorityPlayerId == p1) {
            val simulator = GameSimulator(registry)
            val evaluator = AIPlayer.defaultEvaluator()
            val actions = simulator.getLegalActions(state, p1)

            // Play a land
            val landAction = actions.find { it.actionType == "PlayLand" }
            if (landAction != null) {
                val result = processor.process(state, landAction.action).result
                state = result.state
            }

            // Re-enumerate after land play
            val actionsAfterLand = simulator.getLegalActions(state, p1)
            val castCreature = actionsAfterLand.find {
                it.actionType == "CastSpell" && it.description.contains("Raging Goblin", ignoreCase = true)
            }
            val pass = actionsAfterLand.find { it.actionType == "PassPriority" }

            if (castCreature != null && pass != null) {
                // Simulate both and compare
                val castResult = simulator.simulate(state, castCreature.action)
                val passResult = simulator.simulate(state, pass.action)

                // After casting, the creature should be on the battlefield (stack resolved)
                val creaturesAfterCast = castResult.state.getBattlefield(p1).count { entityId ->
                    castResult.state.projectedState.isCreature(entityId)
                }
                creaturesAfterCast shouldBeGreaterThan 0

                // The stack should be empty (spell resolved)
                castResult.state.stack.size shouldBe 0

                // Cast score should be strictly better than pass score
                val castScore = evaluator.evaluate(castResult.state, castResult.state.projectedState, p1)
                val passScore = evaluator.evaluate(passResult.state, passResult.state.projectedState, p1)
                castScore shouldBeGreaterThan passScore
            }
        }
    }

    test("AI chooses to cast creature over passing in main phase") {
        val registry = createCardRegistry()
        val deck = Deck.of("Mountain" to 10, "Raging Goblin" to 10)
        val (initialState, processor) = initGame(registry, deck)

        val p1 = initialState.turnOrder[0]
        val ai1 = AIPlayer.create(registry, p1)
        val ai2 = AIPlayer.create(registry, initialState.turnOrder[1])

        // Advance to main phase and play a land
        var state: GameState = initialState
        var safety = 0
        while ((state.phase != Phase.PRECOMBAT_MAIN || state.priorityPlayerId != p1) && safety < 50 && !state.gameOver) {
            val next: GameState? = when (state.priorityPlayerId) {
                p1 -> ai1.playPriorityWindow(state, processor)
                else -> ai2.playPriorityWindow(state, processor)
            }
            if (next == null) break
            state = next
            safety++
        }

        if (state.phase == Phase.PRECOMBAT_MAIN && state.priorityPlayerId == p1) {
            // Play a land first
            val simulator = GameSimulator(registry)
            val landAction = simulator.getLegalActions(state, p1).find { it.actionType == "PlayLand" }
            if (landAction != null) {
                state = processor.process(state, landAction.action).result.state
            }

            // Now the AI should choose to cast Raging Goblin (not pass)
            val hasCastableCreature = simulator.getLegalActions(state, p1).any {
                it.actionType == "CastSpell" && it.affordable
            }
            if (state.priorityPlayerId == p1 && hasCastableCreature) {
                val simulator = GameSimulator(registry)
                val evaluator = AIPlayer.defaultEvaluator()
                val actions = simulator.getLegalActions(state, p1)

                // Debug: print all actions and their scores
                val pass = actions.find { it.actionType == "PassPriority" }
                val passResult = if (pass != null) simulator.simulate(state, pass.action) else null
                val passScore = if (passResult != null) {
                    evaluator.evaluate(passResult.state, passResult.state.projectedState, p1)
                } else 0.0

                println("=== AI ACTION SCORES ===")
                println("Pass score: $passScore")
                for (a in actions.filter { it.affordable && it.actionType != "PassPriority" && !it.isManaAbility }) {
                    val result = simulator.simulate(state, a.action)
                    val score = evaluator.evaluate(result.state, result.state.projectedState, p1)
                    val resultType = when (result) {
                        is SimulationResult.Terminal -> "Terminal(stack=${result.state.stack.size})"
                        is SimulationResult.NeedsDecision -> "NeedsDecision(${result.decision::class.simpleName})"
                        is SimulationResult.Illegal -> "Illegal(${result.reason})"
                    }
                    println("  ${a.actionType}(${a.description}): score=$score, result=$resultType")
                }

                val action = ai1.chooseAction(state)
                println("AI CHOSE: ${action::class.simpleName}")
                (action is CastSpell).shouldBeTrue()
            }
        }
    }

    test("board evaluator scores winning state highest") {
        val registry = createCardRegistry()
        val (state, _) = initGame(registry, Deck.of("Mountain" to 20))

        val playerId = state.turnOrder[0]
        val evaluator = AIPlayer.defaultEvaluator()

        val wonState = state.copy(gameOver = true, winnerId = playerId)
        val lostState = state.copy(gameOver = true, winnerId = state.turnOrder[1])

        val wonScore = evaluator.evaluate(wonState, wonState.projectedState, playerId)
        val lostScore = evaluator.evaluate(lostState, lostState.projectedState, playerId)

        wonScore shouldBeGreaterThan lostScore
        wonScore shouldBeGreaterThan 0.0
        (lostScore < 0.0).shouldBeTrue()
    }
})
