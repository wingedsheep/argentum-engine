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
 * Bard the Bowman — {1}{W}{U} 1/3 Legendary Human Archer (HOB #145).
 *
 * "Reach. Whenever you draw your second card each turn, put a +1/+1 counter on target creature.
 *  It gains lifelink until end of turn."
 *
 * The trigger is [com.wingedsheep.sdk.dsl.Triggers.NthCardDrawn]`(2)`; both halves ride the one
 * target, so an illegal target has to fizzle the whole ability.
 */
class BardTheBowmanScenarioTest : ScenarioTestBase() {

    private fun plusOneCounters(game: TestGame, name: String): Int {
        val id = game.findPermanent(name) ?: error("$name not on battlefield")
        return game.state.getEntity(id)?.get<CountersComponent>()
            ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
    }

    init {
        context("Bard the Bowman's second-draw trigger") {

            test("drawing a second card puts a +1/+1 counter on the target and gives it lifelink") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Bard the Bowman")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInHand(1, "Divination")
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withCardsDrawnThisTurn(1, 0)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!

                // Divination draws two; the 2nd crosses N=2 and fires the trigger once.
                game.castSpell(1, "Divination").error shouldBe null
                game.resolveStack()
                game.selectTargets(listOf(bears))
                game.resolveStack()

                withClue("target got exactly one +1/+1 counter") {
                    plusOneCounters(game, "Grizzly Bears") shouldBe 1
                }
                withClue("target gained lifelink until end of turn") {
                    game.state.projectedState.hasKeyword(bears, Keyword.LIFELINK) shouldBe true
                }
                withClue("Bard itself was not the target and got no counter") {
                    plusOneCounters(game, "Bard the Bowman") shouldBe 0
                }
            }

            test("no trigger when the second draw already happened earlier this turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Bard the Bowman")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInHand(1, "Divination")
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    // Already five draws deep this turn — these become the 6th and 7th.
                    .withCardsDrawnThisTurn(1, 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!

                game.castSpell(1, "Divination").error shouldBe null
                game.resolveStack()

                withClue("nothing is waiting on a target — the ability never triggered") {
                    game.hasPendingDecision() shouldBe false
                }
                withClue("no counter and no lifelink") {
                    plusOneCounters(game, "Grizzly Bears") shouldBe 0
                    game.state.projectedState.hasKeyword(bears, Keyword.LIFELINK) shouldBe false
                }
            }

            test("Bard can target a creature an opponent controls — 'target creature' is unrestricted") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Bard the Bowman")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardInHand(1, "Divination")
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withCardsDrawnThisTurn(1, 0)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!

                game.castSpell(1, "Divination").error shouldBe null
                game.resolveStack()
                game.selectTargets(listOf(bears)).error shouldBe null
                game.resolveStack()

                withClue("the opponent's creature got the counter and the lifelink") {
                    plusOneCounters(game, "Grizzly Bears") shouldBe 1
                    game.state.projectedState.hasKeyword(bears, Keyword.LIFELINK) shouldBe true
                }
            }
        }
    }
}
