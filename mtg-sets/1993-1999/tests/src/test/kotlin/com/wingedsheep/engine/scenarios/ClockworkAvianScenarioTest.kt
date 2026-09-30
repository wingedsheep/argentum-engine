package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.atq.cards.ClockworkAvian
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.engine.core.Outcome

class ClockworkAvianScenarioTest : ScenarioTestBase() {

    private val stateProjector = StateProjector()

    init {
        fun plusOneZero(game: TestGame, id: com.wingedsheep.sdk.model.EntityId): Int =
            game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ZERO) ?: 0

        context("Clockwork Avian") {

            test("enters with four +1/+0 counters and is a 4/4") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Clockwork Avian")
                    .withLandsOnBattlefield(1, "Mountain", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Clockwork Avian").error shouldBe null
                game.resolveStack()

                val avian = game.findPermanent("Clockwork Avian")!!
                withClue("Enters with four +1/+0 counters") {
                    plusOneZero(game, avian) shouldBe 4
                }

                val projected = stateProjector.project(game.state)
                withClue("Base 0/4 + four +1/+0 counters projects as a 4/4 (power modified, toughness unchanged)") {
                    projected.getPower(avian) shouldBe 4
                    projected.getToughness(avian) shouldBe 4
                }
            }

            test("removes a +1/+0 counter at end of combat after attacking, becoming a 3/4") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Clockwork Avian")
                    .withActivePlayer(1)
                    .inPhase(Phase.BEGINNING, Step.UNTAP)
                    .build()

                // Stamp the four +1/+0 counters it would have entered with.
                val avian = game.findPermanent("Clockwork Avian")!!
                game.state = game.state.updateEntity(avian) { c ->
                    c.with(CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ZERO to 4)))
                }

                withClue("Starts at 4/4 with four counters") {
                    plusOneZero(game, avian) shouldBe 4
                    stateProjector.project(game.state).getPower(avian) shouldBe 4
                }

                // Advance into combat and attack with the Avian.
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Clockwork Avian" to 2)).error shouldBe null

                // Advance to end of combat; the end-of-combat trigger fires and resolves.
                game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)
                game.resolveStack()

                withClue("One +1/+0 counter shed at end of combat (attacked this combat)") {
                    plusOneZero(game, avian) shouldBe 3
                }
                withClue("With three +1/+0 counters it is a 3/4") {
                    val projected = stateProjector.project(game.state)
                    projected.getPower(avian) shouldBe 3
                    projected.getToughness(avian) shouldBe 4
                }
            }

            test("removes a +1/+0 counter at end of combat after blocking, becoming a 3/4") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Clockwork Avian")
                    .withCardOnBattlefield(2, "Grizzly Bears") // opponent's attacker
                    .withActivePlayer(2)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                // Stamp the four +1/+0 counters it would have entered with.
                val avian = game.findPermanent("Clockwork Avian")!!
                game.state = game.state.updateEntity(avian) { c ->
                    c.with(CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ZERO to 4)))
                }

                // The opponent attacks; player 1's Avian blocks the Bears (it never attacked itself —
                // this exercises the "or blocked this combat" half of the shed trigger).
                game.declareAttackers(mapOf("Grizzly Bears" to 1)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Clockwork Avian" to listOf("Grizzly Bears")))

                // Advance to end of combat; the end-of-combat trigger fires and resolves.
                game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)
                game.resolveStack()

                withClue("One +1/+0 counter shed at end of combat (blocked this combat)") {
                    plusOneZero(game, avian) shouldBe 3
                }
                withClue("With three +1/+0 counters it is a 3/4") {
                    val projected = stateProjector.project(game.state)
                    projected.getPower(avian) shouldBe 3
                    projected.getToughness(avian) shouldBe 4
                }
            }

            test("does not remove a counter at end of combat if it neither attacked nor blocked") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Clockwork Avian")
                    .withActivePlayer(1)
                    .inPhase(Phase.BEGINNING, Step.UNTAP)
                    .build()

                val avian = game.findPermanent("Clockwork Avian")!!
                game.state = game.state.updateEntity(avian) { c ->
                    c.with(CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ZERO to 4)))
                }

                // Pass through combat without declaring it as an attacker.
                game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)
                game.resolveStack()

                withClue("No combat participation → no counter removed (intervening-if fails)") {
                    plusOneZero(game, avian) shouldBe 4
                }
            }
        }
    }

    init {
    val refillAbilityId = ClockworkAvian.activatedAbilities[0].id // {X}, {T}: put up to X +1/+0 counters

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        return driver
    }

    fun plusOneZero(driver: GameTestDriver, id: EntityId): Int =
        driver.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ZERO) ?: 0

    test("refill puts the full X when it stays at or below four total") {
        val driver = createDriver()
        val player = driver.player1
        val avian = driver.putCreatureOnBattlefield(player, "Clockwork Avian")
        driver.removeSummoningSickness(avian)
        // Below the cap: one counter, so X=2 fits entirely (1 + 2 = 3 ≤ 4).
        driver.addComponent(avian, CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ZERO to 1)))

        driver.passPriorityUntil(Step.UPKEEP)
        driver.giveColorlessMana(player, 2)
        driver.submit(
            ActivateAbility(playerId = player, sourceId = avian, abilityId = refillAbilityId, xValue = 2)
        ).outcome shouldBe Outcome.Done
        driver.bothPass()
        val decision = driver.pendingDecision as com.wingedsheep.engine.core.ChooseNumberDecision
        driver.submitDecision(player, com.wingedsheep.engine.core.NumberChosenResponse(decision.id, decision.maxValue)).error shouldBe null

        plusOneZero(driver, avian) shouldBe 3 // 1 + min(2, 4-1) = 1 + 2
    }

    test("refill choice respects the remaining capacity") {
        val driver = createDriver()
        val player = driver.player1
        val avian = driver.putCreatureOnBattlefield(player, "Clockwork Avian")
        driver.removeSummoningSickness(avian)
        // Two counters already; X=5 would overshoot, so only 4-2 = 2 may be added.
        driver.addComponent(avian, CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ZERO to 2)))

        driver.passPriorityUntil(Step.UPKEEP)
        driver.giveColorlessMana(player, 5)
        driver.submit(
            ActivateAbility(playerId = player, sourceId = avian, abilityId = refillAbilityId, xValue = 5)
        ).outcome shouldBe Outcome.Done
        driver.bothPass()
        val decision = driver.pendingDecision as com.wingedsheep.engine.core.ChooseNumberDecision
        driver.submitDecision(player, com.wingedsheep.engine.core.NumberChosenResponse(decision.id, decision.maxValue)).error shouldBe null

        plusOneZero(driver, avian) shouldBe 4 // capped at four total, NOT 2 + 5 = 7
    }

    test("refill cannot be activated outside your upkeep") {
        val driver = createDriver()
        val player = driver.player1
        val avian = driver.putCreatureOnBattlefield(player, "Clockwork Avian")
        driver.removeSummoningSickness(avian)
        driver.addComponent(avian, CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ZERO to 1)))

        // In the main phase (not upkeep) the DuringStep(UPKEEP) restriction makes activation illegal.
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.giveColorlessMana(player, 2)
        driver.submitExpectFailure(
            ActivateAbility(playerId = player, sourceId = avian, abilityId = refillAbilityId, xValue = 2)
        )

        plusOneZero(driver, avian) shouldBe 1 // unchanged — the ability never resolved
    }
    }
}
