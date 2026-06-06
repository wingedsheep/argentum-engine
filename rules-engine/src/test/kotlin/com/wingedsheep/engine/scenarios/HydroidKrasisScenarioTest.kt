package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Hydroid Krasis (RNA #183) — {X}{G}{U} Creature — Jellyfish Hydra Beast 0/0.
 *
 * "When you cast this spell, you gain half X life and draw half X cards. Round down each time.
 *  Flying, trample
 *  This creature enters with X +1/+1 counters on it."
 *
 * The proof that the cast-time X survives onto the permanent via DynamicAmount.CastX: the cast
 * trigger (gain/draw half X) and the enters-with-counters replacement (X +1/+1 counters) both read
 * the same X chosen as the spell was cast — the cast trigger off the stack object, the counters off
 * the durable component on the entering permanent.
 */
class HydroidKrasisScenarioTest : ScenarioTestBase() {

    private val projector = StateProjector()

    private fun plusOneCounters(game: TestGame, name: String): Int {
        val permanent = game.findPermanent(name)!!
        return game.state.getEntity(permanent)?.get<CountersComponent>()
            ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
    }

    init {
        context("Hydroid Krasis") {

            test("cast for X=6: gain 3 life, draw 3 cards, enters as a 6/6 with six +1/+1 counters") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Hydroid Krasis")
                    .withLandsOnBattlefield(1, "Forest", 8)
                    .withLandsOnBattlefield(1, "Island", 8)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(1, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val handBefore = game.handSize(1)
                game.castXSpell(1, "Hydroid Krasis", xValue = 6).error shouldBe null
                game.resolveStack()

                withClue("half of 6, rounded down, is 3 life gained") {
                    game.getLifeTotal(1) shouldBe 23
                }
                // Hand: started with N (Hydroid removed on cast) then drew 3.
                withClue("half of 6, rounded down, is 3 cards drawn") {
                    game.handSize(1) shouldBe (handBefore - 1 + 3)
                }
                withClue("enters with six +1/+1 counters") {
                    plusOneCounters(game, "Hydroid Krasis") shouldBe 6
                }
                val hydroid = game.findPermanent("Hydroid Krasis")!!
                withClue("0/0 base + six +1/+1 counters = 6/6") {
                    projector.getProjectedPower(game.state, hydroid) shouldBe 6
                    projector.getProjectedToughness(game.state, hydroid) shouldBe 6
                }
            }

            test("cast for X=5: gain 2 life and draw 2 cards (round down each independently)") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Hydroid Krasis")
                    .withLandsOnBattlefield(1, "Forest", 8)
                    .withLandsOnBattlefield(1, "Island", 8)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(1, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val handBefore = game.handSize(1)
                game.castXSpell(1, "Hydroid Krasis", xValue = 5).error shouldBe null
                game.resolveStack()

                withClue("half of 5, rounded down, is 2") {
                    game.getLifeTotal(1) shouldBe 22
                    game.handSize(1) shouldBe (handBefore - 1 + 2)
                    plusOneCounters(game, "Hydroid Krasis") shouldBe 5
                }
            }

            test("cast for X=0: no life gained, no cards drawn, 0/0 dies as a state-based action") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Hydroid Krasis")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val handBefore = game.handSize(1)
                game.castXSpell(1, "Hydroid Krasis", xValue = 0).error shouldBe null
                game.resolveStack()

                withClue("half of 0 is 0 — no life gained") {
                    game.getLifeTotal(1) shouldBe 20
                }
                withClue("half of 0 is 0 — no cards drawn (only the cast Hydroid left hand)") {
                    game.handSize(1) shouldBe (handBefore - 1)
                }
                withClue("a 0/0 with no counters dies to state-based actions") {
                    game.findPermanent("Hydroid Krasis") shouldBe null
                }
            }
        }
    }
}
