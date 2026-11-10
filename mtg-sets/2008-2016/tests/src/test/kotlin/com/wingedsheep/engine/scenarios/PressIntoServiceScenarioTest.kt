package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Press into Service (OGW #114, {4}{R}, Sorcery).
 *
 *   Support 2. (Put a +1/+1 counter on each of up to two target creatures.)
 *   Gain control of target creature until end of turn. Untap that creature. It gains haste until
 *   end of turn.
 *
 * Two target declarations bound to separate slots: the counters land only on the support targets,
 * the threaten half acts only on its own target, and (per the rulings) one creature may be both.
 */
class PressIntoServiceScenarioTest : ScenarioTestBase() {

    private fun ScenarioTestBase.TestGame.plusOneCounters(id: EntityId) =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun ScenarioTestBase.TestGame.isTapped(id: EntityId) =
        state.getEntity(id)?.get<TappedComponent>() != null

    private fun board() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, "Press into Service")
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withCardOnBattlefield(1, "Hill Giant")
        .withCardOnBattlefield(2, "Craw Wurm", tapped = true)
        .withLandsOnBattlefield(1, "Mountain", 5)
        .withCardInLibrary(1, "Mountain")
        .withCardInLibrary(2, "Mountain")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("support counters go only to the support targets; the threaten target is stolen, untapped and hasted") {
            val game = board()
            val bears = game.findPermanent("Grizzly Bears")!!
            val giant = game.findPermanent("Hill Giant")!!
            val wurm = game.findPermanent("Craw Wurm")!!

            val cast = game.execute(
                CastSpell(
                    game.player1Id, game.findCardsInHand(1, "Press into Service").first(),
                    targets = listOf(
                        ChosenTarget.Permanent(bears), ChosenTarget.Permanent(giant), ChosenTarget.Permanent(wurm),
                    ),
                    targetGroupCounts = listOf(2, 1),
                )
            )
            withClue("cast: ${cast.error}") { cast.error shouldBe null }
            game.resolveStack()

            withClue("each support target got exactly one counter; the stolen creature got none") {
                game.plusOneCounters(bears) shouldBe 1
                game.plusOneCounters(giant) shouldBe 1
                game.plusOneCounters(wurm) shouldBe 0
            }
            val projected = game.state.projectedState
            withClue("the Craw Wurm changed hands, untapped, with haste") {
                projected.getController(wurm) shouldBe game.player1Id
                game.isTapped(wurm) shouldBe false
                projected.hasKeyword(wurm, Keyword.HASTE) shouldBe true
            }
            withClue("the support targets were not stolen or hasted") {
                projected.hasKeyword(bears, Keyword.HASTE) shouldBe false
                projected.hasKeyword(giant, Keyword.HASTE) shouldBe false
            }
        }

        test("the same creature can be both a support target and the threaten target") {
            val game = board()
            val wurm = game.findPermanent("Craw Wurm")!!

            val cast = game.execute(
                CastSpell(
                    game.player1Id, game.findCardsInHand(1, "Press into Service").first(),
                    targets = listOf(ChosenTarget.Permanent(wurm), ChosenTarget.Permanent(wurm)),
                    targetGroupCounts = listOf(1, 1),
                )
            )
            withClue("cast: ${cast.error}") { cast.error shouldBe null }
            game.resolveStack()

            game.plusOneCounters(wurm) shouldBe 1
            game.state.projectedState.getController(wurm) shouldBe game.player1Id
            game.isTapped(wurm) shouldBe false
            game.state.projectedState.hasKeyword(wurm, Keyword.HASTE) shouldBe true
        }

        test("with no support targets, the threaten half still resolves") {
            val game = board()
            val wurm = game.findPermanent("Craw Wurm")!!

            val cast = game.execute(
                CastSpell(
                    game.player1Id, game.findCardsInHand(1, "Press into Service").first(),
                    targets = listOf(ChosenTarget.Permanent(wurm)),
                    targetGroupCounts = listOf(0, 1),
                )
            )
            withClue("cast: ${cast.error}") { cast.error shouldBe null }
            game.resolveStack()

            game.plusOneCounters(wurm) shouldBe 0
            game.state.projectedState.getController(wurm) shouldBe game.player1Id
            game.isTapped(wurm) shouldBe false
        }
    }
}
