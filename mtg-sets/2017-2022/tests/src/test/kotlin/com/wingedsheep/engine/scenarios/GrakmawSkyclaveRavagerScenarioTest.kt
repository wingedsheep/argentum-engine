package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Grakmaw, Skyclave Ravager (ZNR #223).
 *
 * Both dies triggers read last-known information: the "another creature" intervening-if must read
 * the *dying creature's* +1/+1 count (not Grakmaw's), and the token's X must read Grakmaw's own
 * count as it last existed — including the ruling that -1/-1 counters which killed it don't
 * reduce X (it dies before the counters annihilate).
 */
class GrakmawSkyclaveRavagerScenarioTest : ScenarioTestBase() {

    private val testExecution = card("Grakmaw Test Execution") {
        manaCost = "{0}"
        typeLine = "Instant"
        spell {
            val victim = target(TargetFilter.Creature)
            effect = Effects.Destroy(victim)
        }
    }

    private val testWither = card("Grakmaw Test Wither") {
        manaCost = "{0}"
        typeLine = "Instant"
        spell {
            val victim = target(TargetFilter.Creature)
            effect = Effects.AddCounters(CounterType.MINUS_ONE_MINUS_ONE, 3, victim)
        }
    }

    init {
        cardRegistry.register(testExecution)
        cardRegistry.register(testWither)

        fun board(bearsCounter: Boolean): TestGame {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Grakmaw, Skyclave Ravager")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(1, "Grakmaw Test Execution")
                .withCardInHand(1, "Grakmaw Test Wither")
                .withCardInLibrary(1, "Swamp")
                .withCardInLibrary(2, "Swamp")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val grakmaw = game.findPermanent("Grakmaw, Skyclave Ravager")!!
            game.state = game.state.updateEntity(grakmaw) { c ->
                c.with(CountersComponent().withAdded(CounterType.PLUS_ONE_PLUS_ONE, 3))
            }
            if (bearsCounter) {
                val bears = game.findPermanent("Grizzly Bears")!!
                game.state = game.state.updateEntity(bears) { c ->
                    c.with(CountersComponent().withAdded(CounterType.PLUS_ONE_PLUS_ONE, 1))
                }
            }
            return game
        }

        fun TestGame.grakmawCounters(): Int {
            val grakmaw = findPermanent("Grakmaw, Skyclave Ravager")!!
            return state.getEntity(grakmaw)?.get<CountersComponent>()
                ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
        }

        context("Grakmaw, Skyclave Ravager") {

            test("another creature dying with a +1/+1 counter puts one counter on Grakmaw") {
                val game = board(bearsCounter = true)
                val bears = game.findPermanent("Grizzly Bears")!!

                game.castSpell(1, "Grakmaw Test Execution", targetId = bears).error shouldBe null
                game.resolveStack()

                withClue("Bears died") { game.isInGraveyard(1, "Grizzly Bears") shouldBe true }
                withClue("the dying creature's last-known counter satisfies the intervening-if") {
                    game.grakmawCounters() shouldBe 4
                }
            }

            test("another creature dying without a +1/+1 counter does nothing") {
                val game = board(bearsCounter = false)
                val bears = game.findPermanent("Grizzly Bears")!!

                game.castSpell(1, "Grakmaw Test Execution", targetId = bears).error shouldBe null
                game.resolveStack()

                withClue("Bears died") { game.isInGraveyard(1, "Grizzly Bears") shouldBe true }
                withClue("Grakmaw's own counters don't satisfy the condition") {
                    game.grakmawCounters() shouldBe 3
                }
            }

            test("Grakmaw dying creates an X/X Hydra from its last-known +1/+1 counters") {
                val game = board(bearsCounter = false)
                val grakmaw = game.findPermanent("Grakmaw, Skyclave Ravager")!!

                game.castSpell(1, "Grakmaw Test Execution", targetId = grakmaw).error shouldBe null
                game.resolveStack()

                withClue("Grakmaw died") { game.isInGraveyard(1, "Grakmaw, Skyclave Ravager") shouldBe true }
                val hydra = game.findPermanents("Hydra Token").single()
                game.state.projectedState.getPower(hydra) shouldBe 3
                game.state.projectedState.getToughness(hydra) shouldBe 3
            }

            test("-1/-1 counters that kill Grakmaw don't shrink X") {
                val game = board(bearsCounter = false)
                val grakmaw = game.findPermanent("Grakmaw, Skyclave Ravager")!!

                game.castSpell(1, "Grakmaw Test Wither", targetId = grakmaw).error shouldBe null
                game.resolveStack()

                withClue("Grakmaw died at 0/0") {
                    game.isInGraveyard(1, "Grakmaw, Skyclave Ravager") shouldBe true
                }
                val hydra = game.findPermanents("Hydra Token").single()
                withClue("X counts the +1/+1 counters it had before they annihilated") {
                    game.state.projectedState.getPower(hydra) shouldBe 3
                    game.state.projectedState.getToughness(hydra) shouldBe 3
                }
            }
        }
    }
}
