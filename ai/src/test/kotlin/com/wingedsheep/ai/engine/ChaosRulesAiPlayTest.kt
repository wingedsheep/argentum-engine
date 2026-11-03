package com.wingedsheep.ai.engine

import com.wingedsheep.engine.core.GameConfig
import com.wingedsheep.engine.core.GameInitializer
import com.wingedsheep.engine.core.PlayerConfig
import com.wingedsheep.engine.core.ActionProcessor
import com.wingedsheep.engine.core.EngineServices
import com.wingedsheep.engine.mechanics.chaos.ChaosVariant
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.ChosenModesEverComponent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.sdk.chaos.ChaosRules
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.basicLand
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Every chaos rule ([ChaosRules]) plays through with the engine AI making every choice it asks for —
 * targets, cards to discard or sacrifice, numbers, yes/no, ordering — so a rule the AI can't answer,
 * or that errors when it resolves, fails here rather than in a live game. A short run of whole
 * AI-vs-AI Chaos games then checks the variant end to end: every upkeep rolls, games finish, and no
 * player rolls a rule twice.
 */
class ChaosRulesAiPlayTest : FunSpec({

    val lands = listOf("Plains", "Island", "Swamp", "Mountain", "Forest").map { basicLand(it) {} }
    val cards = lands + listOf(
        card("Chaos Bears") { manaCost = "{1}{G}"; typeLine = "Creature — Bear"; power = 2; toughness = 2 },
        card("Chaos Giant") { manaCost = "{3}{R}"; typeLine = "Creature — Giant"; power = 3; toughness = 3 },
        card("Chaos Drake") {
            manaCost = "{2}{U}"; typeLine = "Creature — Drake"; power = 2; toughness = 2
            oracleText = "Flying"; keywords(Keyword.FLYING)
        },
        card("Chaos Thopter") {
            manaCost = "{0}"; typeLine = "Artifact Creature — Thopter"; power = 0; toughness = 2
            oracleText = "Flying"; keywords(Keyword.FLYING)
        },
        card("Chaos Juggernaut") { manaCost = "{4}"; typeLine = "Artifact Creature — Juggernaut"; power = 5; toughness = 3 },
        card("Chaos Knight") { manaCost = "{B}{B}"; typeLine = "Creature — Human Knight"; power = 2; toughness = 2 },
        card("Chaos Bolt") {
            manaCost = "{R}"; typeLine = "Instant"; oracleText = "Chaos Bolt deals 3 damage to any target."
            spell { val t = target(Targets.Any); effect = Effects.DealDamage(3, t) }
        },
        card("Chaos Insight") {
            manaCost = "{2}{U}"; typeLine = "Sorcery"; oracleText = "Draw two cards."
            spell { effect = Effects.DrawCards(2) }
        },
        card("Chaos Charm") { manaCost = "{1}{W}"; typeLine = "Enchantment" },
        card("Chaos Walker") { manaCost = "{2}{W}"; typeLine = "Legendary Planeswalker — Test"; startingLoyalty = 3 },
    )
    val deck = Deck.of(
        "Plains" to 3, "Island" to 4, "Swamp" to 3, "Mountain" to 4, "Forest" to 4,
        "Chaos Bears" to 4, "Chaos Giant" to 4, "Chaos Bolt" to 4, "Chaos Insight" to 2, "Chaos Drake" to 4,
        "Chaos Thopter" to 2, "Chaos Juggernaut" to 2,
    )

    /** A board where nearly every rule has something to act on: creatures, artifacts, lands of every type, graveyards. */
    fun GameTestDriver.busyBoard() {
        for (p in listOf(player1, player2)) {
            for (land in listOf("Plains", "Island", "Swamp", "Mountain", "Forest")) putLandOnBattlefield(p, land)
            for (name in listOf("Chaos Bears", "Chaos Drake", "Chaos Thopter", "Chaos Juggernaut", "Chaos Knight")) {
                putCreatureOnBattlefield(p, name)
            }
            putPermanentOnBattlefield(p, "Chaos Charm")
            for (name in listOf("Chaos Giant", "Chaos Bears", "Chaos Bolt", "Chaos Insight")) putCardInGraveyard(p, name)
        }
        val walker = putPermanentOnBattlefield(player1, "Chaos Walker")
        replaceState(state.updateEntity(walker) { it.with(CountersComponent(mapOf(CounterType.LOYALTY to 3))) })
    }

    fun spend(state: GameState, emblem: EntityId, indices: Set<Int>): GameState =
        state.updateEntity(emblem) { it.with(ChosenModesEverComponent(indices)) }

    test("every chaos rule resolves with the AI making its choices") {
        val all = ChaosRules.all.indices.toSet()
        for ((index, rule) in ChaosRules.all.withIndex()) {
            withClue("${rule.id} ${rule.name}") {
                val driver = GameTestDriver()
                driver.registerCards(cards)
                driver.initMirrorMatch(deck)
                driver.busyBoard()
                var state = ChaosVariant.install(driver.state, listOf(driver.player1, driver.player2))
                // Player 1's emblem has nothing left; player 2's can roll only this rule.
                state = spend(state, ChaosVariant.emblemOf(state, driver.player1)!!, all)
                val emblem = ChaosVariant.emblemOf(state, driver.player2)!!
                state = spend(state, emblem, all - index)
                driver.replaceState(state)
                val ais = listOf(driver.player1, driver.player2).associateWith { AIPlayer.create(driver.cardRegistry, it) }

                driver.passPriorityUntil(Step.UPKEEP, driver.player2)
                var rolled: List<Int>? = null
                var steps = 0
                // Play until the rule — and anything it set up for later this turn — has happened:
                // player 1's next upkeep, where the stack is empty again.
                while (steps++ < 600 && !driver.state.gameOver) {
                    val s = driver.state
                    if (rolled == null) {
                        rolled = s.stack.mapNotNull { s.getEntity(it)?.get<TriggeredAbilityOnStackComponent>() }
                            .firstOrNull { it.sourceId == emblem }?.chosenModes
                    }
                    if (s.step == Step.UPKEEP && s.activePlayerId == driver.player1 && s.stack.isEmpty() && s.pendingDecision == null) break
                    val decision = s.pendingDecision
                    val result = when {
                        decision != null ->
                            driver.submitDecision(decision.playerId, ais.getValue(decision.playerId).respondToDecision(s, decision))
                        s.priorityPlayerId != null && s.step == Step.DECLARE_ATTACKERS && s.priorityPlayerId == s.activePlayerId ->
                            driver.submit(ais.getValue(s.priorityPlayerId!!).chooseAction(s))
                        s.priorityPlayerId != null -> {
                            driver.autoSubmitCombatDeclarationIfNeeded()
                            driver.passPriority(driver.state.priorityPlayerId ?: break)
                        }
                        else -> error("No one can act at ${s.step}")
                    }
                    result.error shouldBe null
                }
                rolled shouldBe listOf(index)
            }
        }
    }

    test("whole AI-vs-AI Chaos games roll every upkeep and never repeat a rule") {
        val registry = CardRegistry().apply { register(cards) }
        for (seed in 1L..4L) {
            withClue("seed $seed") {
                val result = GameInitializer(registry).initializeGame(
                    GameConfig(
                        players = listOf(PlayerConfig("A", deck), PlayerConfig("B", deck)),
                        skipMulligans = true, seed = seed, chaosRules = true,
                    )
                )
                val processor = ActionProcessor(EngineServices(registry))
                var state = processor.beginGame(result.state, result.events).state
                val emblems = ChaosVariant.emblems(state).values.toSet()
                val factory = AIPlayer.Factory(registry)
                val ais = result.playerIds.associateWith { factory.create(it) }
                val seen = mutableSetOf<EntityId>()
                val rolled = mutableMapOf<EntityId, MutableList<Int>>()
                var windows = 0
                while (!state.gameOver && state.turnNumber <= 16 && windows++ < 5000) {
                    for (id in state.stack) {
                        val t = state.getEntity(id)?.get<TriggeredAbilityOnStackComponent>() ?: continue
                        if (t.sourceId in emblems && t.chosenModes.isNotEmpty() && seen.add(id)) {
                            rolled.getOrPut(t.controllerId) { mutableListOf() } += t.chosenModes.single()
                        }
                    }
                    val actor = state.pendingDecision?.playerId ?: state.priorityPlayerId ?: error("No one can act at ${state.step}")
                    state = ais.getValue(actor).playPriorityWindow(state, processor)
                }
                (rolled.values.sumOf { it.size } >= minOf(state.turnNumber, 16) - 1) shouldBe true
                for (rules in rolled.values) rules.toSet().size shouldBe rules.size
            }
        }
    }
})
