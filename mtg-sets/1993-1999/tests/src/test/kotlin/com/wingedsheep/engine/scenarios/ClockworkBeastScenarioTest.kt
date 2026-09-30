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
import com.wingedsheep.mtg.sets.definitions.lea.cards.ClockworkBeast
import com.wingedsheep.sdk.model.Deck

class ClockworkBeastScenarioTest : ScenarioTestBase() {

    private val stateProjector = StateProjector()

    init {
        fun plusOneZero(game: TestGame, id: com.wingedsheep.sdk.model.EntityId): Int =
            game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ZERO) ?: 0

        context("Clockwork Beast") {

            test("enters with seven +1/+0 counters and is a 7/4") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Clockwork Beast")
                    .withLandsOnBattlefield(1, "Mountain", 6)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Clockwork Beast").error shouldBe null
                game.resolveStack()

                val avian = game.findPermanent("Clockwork Beast")!!
                withClue("Enters with seven +1/+0 counters") {
                    plusOneZero(game, avian) shouldBe 7
                }

                val projected = stateProjector.project(game.state)
                withClue("Base 0/4 + seven +1/+0 counters projects as a 7/4 (power modified, toughness unchanged)") {
                    projected.getPower(avian) shouldBe 7
                    projected.getToughness(avian) shouldBe 4
                }
            }

            test("removes a +1/+0 counter at end of combat after attacking, becoming a 6/4") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Clockwork Beast")
                    .withActivePlayer(1)
                    .inPhase(Phase.BEGINNING, Step.UNTAP)
                    .build()

                // Stamp the seven +1/+0 counters it would have entered with.
                val avian = game.findPermanent("Clockwork Beast")!!
                game.state = game.state.updateEntity(avian) { c ->
                    c.with(CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ZERO to 7)))
                }

                withClue("Starts at 7/4 with seven counters") {
                    plusOneZero(game, avian) shouldBe 7
                    stateProjector.project(game.state).getPower(avian) shouldBe 7
                }

                // Advance into combat and attack with the Avian.
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Clockwork Beast" to 2)).error shouldBe null

                // Advance to end of combat; the end-of-combat trigger fires and resolves.
                game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)
                game.resolveStack()

                withClue("One +1/+0 counter shed at end of combat (attacked this combat)") {
                    plusOneZero(game, avian) shouldBe 6
                }
                withClue("With three +1/+0 counters it is a 6/4") {
                    val projected = stateProjector.project(game.state)
                    projected.getPower(avian) shouldBe 6
                    projected.getToughness(avian) shouldBe 4
                }
            }

            test("removes a +1/+0 counter at end of combat after blocking, becoming a 6/4") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Clockwork Beast")
                    .withCardOnBattlefield(2, "Grizzly Bears") // opponent's attacker
                    .withActivePlayer(2)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                // Stamp the seven +1/+0 counters it would have entered with.
                val avian = game.findPermanent("Clockwork Beast")!!
                game.state = game.state.updateEntity(avian) { c ->
                    c.with(CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ZERO to 7)))
                }

                // The opponent attacks; player 1's Avian blocks the Bears (it never attacked itself —
                // this exercises the "or blocked this combat" half of the shed trigger).
                game.declareAttackers(mapOf("Grizzly Bears" to 1)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Clockwork Beast" to listOf("Grizzly Bears")))

                // Advance to end of combat; the end-of-combat trigger fires and resolves.
                game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)
                game.resolveStack()

                withClue("One +1/+0 counter shed at end of combat (blocked this combat)") {
                    plusOneZero(game, avian) shouldBe 6
                }
                withClue("With three +1/+0 counters it is a 6/4") {
                    val projected = stateProjector.project(game.state)
                    projected.getPower(avian) shouldBe 6
                    projected.getToughness(avian) shouldBe 4
                }
            }

            test("does not remove a counter at end of combat if it neither attacked nor blocked") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Clockwork Beast")
                    .withActivePlayer(1)
                    .inPhase(Phase.BEGINNING, Step.UNTAP)
                    .build()

                val avian = game.findPermanent("Clockwork Beast")!!
                game.state = game.state.updateEntity(avian) { c ->
                    c.with(CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ZERO to 7)))
                }

                // Pass through combat without declaring it as an attacker.
                game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)
                game.resolveStack()

                withClue("No combat participation → no counter removed (intervening-if fails)") {
                    plusOneZero(game, avian) shouldBe 7
                }
            }
        }
    }


    init {
        fun refill(initial: Int, x: Int, chosen: Int, expected: Int) {
            val driver = GameTestDriver()
            driver.registerCards(TestCards.all)
            driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
            val player = driver.player1
            val beast = driver.putCreatureOnBattlefield(player, "Clockwork Beast")
            driver.removeSummoningSickness(beast)
            driver.addComponent(beast, CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ZERO to initial)))
            driver.passPriorityUntil(Step.UPKEEP)
            driver.giveColorlessMana(player, x)
            driver.submit(ActivateAbility(player, beast, ClockworkBeast.activatedAbilities.first().id, xValue = x)).error shouldBe null
            driver.bothPass()
            val decision = driver.pendingDecision as com.wingedsheep.engine.core.ChooseNumberDecision
            decision.minValue shouldBe 0
            decision.maxValue shouldBe minOf(x, (7 - initial).coerceAtLeast(0))
            driver.submitDecision(player, com.wingedsheep.engine.core.NumberChosenResponse(decision.id, decision.maxValue + 1)).error.isNullOrEmpty() shouldBe false
            driver.submitDecision(player, com.wingedsheep.engine.core.NumberChosenResponse(decision.id, chosen)).error shouldBe null
            driver.state.getEntity(beast)!!.get<CountersComponent>()!!.getCount(CounterType.PLUS_ONE_PLUS_ZERO) shouldBe expected
        }
        test("refill permits fewer counters than X") { refill(2, 4, 1, 3) }
        test("refill permits zero counters") { refill(2, 4, 0, 2) }
        test("refill stops at seven") { refill(5, 5, 2, 7) }
        test("refill preserves an existing total above seven") { refill(9, 3, 0, 9) }
        test("zero X adds nothing") { refill(2, 0, 0, 2) }
        test("refill is restricted to its controllers upkeep") {
            val driver = GameTestDriver()
            driver.registerCards(TestCards.all)
            driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
            val player = driver.player1
            val beast = driver.putCreatureOnBattlefield(player, "Clockwork Beast")
            driver.removeSummoningSickness(beast)
            driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
            driver.giveColorlessMana(player, 1)
            driver.submitExpectFailure(ActivateAbility(player, beast, ClockworkBeast.activatedAbilities.first().id, xValue = 1))
        }

        test("combat decay still happens when Fog prevents damage") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Clockwork Beast")
                .withCardInHand(1, "Fog")
                .withLandsOnBattlefield(1, "Forest", 1)
                .withActivePlayer(1)
                .inPhase(Phase.BEGINNING, Step.UNTAP)
                .build()
            val beast = game.findPermanent("Clockwork Beast")!!
            game.state = game.state.updateEntity(beast) { it.with(CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ZERO to 7))) }
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Clockwork Beast" to 2)).error shouldBe null
            game.castSpell(1, "Fog").error shouldBe null
            game.resolveStack()
            game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)
            game.resolveStack()
            game.state.getEntity(beast)!!.get<CountersComponent>()!!.getCount(CounterType.PLUS_ONE_PLUS_ZERO) shouldBe 6
        }
        test("a Beast with no counters can attack and survive combat decay") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Clockwork Beast")
                .withActivePlayer(1)
                .inPhase(Phase.BEGINNING, Step.UNTAP)
                .build()
            val beast = game.findPermanent("Clockwork Beast")!!
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Clockwork Beast" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)
            game.resolveStack()
            game.state.getEntity(beast)!!.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ZERO) ?: 0 shouldBe 0
        }

        test("refill measures available capacity at resolution") {
            val driver = GameTestDriver()
            driver.registerCards(TestCards.all)
            driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
            val player = driver.player1
            val beast = driver.putCreatureOnBattlefield(player, "Clockwork Beast")
            driver.removeSummoningSickness(beast)
            driver.addComponent(beast, CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ZERO to 2)))
            driver.passPriorityUntil(Step.UPKEEP)
            driver.giveColorlessMana(player, 4)
            driver.submit(ActivateAbility(player, beast, ClockworkBeast.activatedAbilities.first().id, xValue = 4)).error shouldBe null
            driver.addComponent(beast, CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ZERO to 6)))
            driver.bothPass()
            val decision = driver.pendingDecision as com.wingedsheep.engine.core.ChooseNumberDecision
            decision.maxValue shouldBe 1
            driver.submitDecision(player, com.wingedsheep.engine.core.NumberChosenResponse(decision.id, 1)).error shouldBe null
            driver.state.getEntity(beast)!!.get<CountersComponent>()!!.getCount(CounterType.PLUS_ONE_PLUS_ZERO) shouldBe 7
        }

        test("refill does not prompt after its source leaves") {
            val driver = GameTestDriver()
            driver.registerCards(TestCards.all)
            driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
            val player = driver.player1
            val beast = driver.putCreatureOnBattlefield(player, "Clockwork Beast")
            driver.removeSummoningSickness(beast)
            driver.passPriorityUntil(Step.UPKEEP)
            driver.giveColorlessMana(player, 3)
            driver.submit(ActivateAbility(player, beast, ClockworkBeast.activatedAbilities.first().id, xValue = 3)).error shouldBe null
            driver.replaceState(driver.state.removeEntity(beast))
            driver.bothPass()
            driver.pendingDecision shouldBe null
            driver.state.stack shouldBe emptyList()
        }

        test("a returning source is a new object and does not receive the old refill") {
            val driver = GameTestDriver()
            driver.registerCards(TestCards.all)
            driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
            val player = driver.player1
            val beast = driver.putCreatureOnBattlefield(player, "Clockwork Beast")
            driver.removeSummoningSickness(beast)
            driver.passPriorityUntil(Step.UPKEEP)
            driver.giveColorlessMana(player, 3)
            driver.submit(ActivateAbility(player, beast, ClockworkBeast.activatedAbilities.first().id, xValue = 3)).error shouldBe null
            val battlefield = com.wingedsheep.engine.state.ZoneKey(player, com.wingedsheep.sdk.core.Zone.BATTLEFIELD)
            val graveyard = com.wingedsheep.engine.state.ZoneKey(player, com.wingedsheep.sdk.core.Zone.GRAVEYARD)
            driver.replaceState(driver.state.moveToZone(beast, battlefield, graveyard).moveToZone(beast, graveyard, battlefield))
            driver.bothPass()
            driver.pendingDecision shouldBe null
            driver.state.stack shouldBe emptyList()
        }
    }
}
