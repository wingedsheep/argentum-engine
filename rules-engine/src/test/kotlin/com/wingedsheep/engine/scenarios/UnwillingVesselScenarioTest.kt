package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Unwilling Vessel (DSK #81) — {2}{U} 3/2 Creature — Human Wizard.
 *
 * "Vigilance
 *  Eerie — Whenever an enchantment you control enters and whenever you fully unlock a Room, put a
 *  possession counter on this creature.
 *  When this creature dies, create an X/X blue Spirit creature token with flying, where X is the
 *  number of counters on this creature."
 *
 * (a) The Eerie enchantment-enters trigger adds a possession counter.
 * (b) The dies trigger creates a single X/X flying Spirit token, X = the creature's total
 *     last-known counter count.
 */
class UnwillingVesselScenarioTest : ScenarioTestBase() {

    init {
        fun possession(game: TestGame, id: com.wingedsheep.sdk.model.EntityId): Int =
            game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.POSSESSION) ?: 0

        context("Unwilling Vessel — Eerie possession counter") {

            test("an enchantment you control entering puts a possession counter on it") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Unwilling Vessel", summoningSickness = false)
                    .withCardInHand(1, "Test Enchantment") // {1}{W}
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val vessel = game.findPermanent("Unwilling Vessel")!!
                withClue("Starts with no possession counters") { possession(game, vessel) shouldBe 0 }

                game.castSpell(1, "Test Enchantment").error shouldBe null
                game.resolveStack()

                withClue("Eerie added one possession counter") { possession(game, vessel) shouldBe 1 }
            }

            test("an opponent's enchantment entering does not add a counter") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Unwilling Vessel", summoningSickness = false)
                    .withCardInHand(2, "Test Enchantment")
                    .withLandsOnBattlefield(2, "Plains", 2)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val vessel = game.findPermanent("Unwilling Vessel")!!

                game.castSpell(2, "Test Enchantment").error shouldBe null
                game.resolveStack()

                withClue("No counter — the enchantment isn't yours") { possession(game, vessel) shouldBe 0 }
            }
        }

        context("Unwilling Vessel — dies token") {

            test("dying with two counters leaves a 2/2 flying blue Spirit token") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Unwilling Vessel", summoningSickness = false)
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val vessel = game.findPermanent("Unwilling Vessel")!!
                // Stamp two possession counters (Vessel is a 3/2; a 3-damage bolt still kills it).
                game.state = game.state.updateEntity(vessel) {
                    it.with(CountersComponent(mapOf(CounterType.POSSESSION to 2)))
                }

                game.castSpell(1, "Lightning Bolt", vessel)
                game.resolveStack()

                withClue("Unwilling Vessel is dead") {
                    game.findPermanent("Unwilling Vessel") shouldBe null
                }

                val token = game.findPermanent("Spirit Token")
                    ?: error("expected a Spirit token to be created")
                withClue("The token is a 2/2 (X = two counters) with flying") {
                    game.state.projectedState.getPower(token) shouldBe 2
                    game.state.projectedState.getToughness(token) shouldBe 2
                    game.state.projectedState.hasKeyword(token, Keyword.FLYING) shouldBe true
                }
            }

            test("dying with no counters leaves a 0/0 token that dies as a state-based action") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Unwilling Vessel", summoningSickness = false)
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val vessel = game.findPermanent("Unwilling Vessel")!!

                game.castSpell(1, "Lightning Bolt", vessel)
                game.resolveStack()

                withClue("Unwilling Vessel is dead") {
                    game.findPermanent("Unwilling Vessel") shouldBe null
                }
                // A 0/0 Spirit token is created but immediately dies to state-based actions, so no
                // Spirit token survives on the battlefield.
                withClue("No surviving Spirit token (a 0/0 dies immediately)") {
                    game.findPermanent("Spirit Token") shouldBe null
                }
            }
        }
    }
}
