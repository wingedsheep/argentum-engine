package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Glimpse the Sun God (BNG #13) — "Tap X target creatures. Scry 1."
 *
 * X sets the exact number of targets: an X=2 cast taps both chosen creatures, leaves an untargeted
 * one alone, then scries 1; a target count that doesn't match X is rejected.
 */
class GlimpseTheSunGodScenarioTest : ScenarioTestBase() {

    private fun board() = scenario()
        .withPlayers("P1", "P2")
        .withCardInHand(1, "Glimpse the Sun God")
        .withLandsOnBattlefield(1, "Plains", 3)
        .withCardOnBattlefield(2, "Grizzly Bears")
        .withCardOnBattlefield(2, "Hill Giant")
        .withCardOnBattlefield(2, "Minotaur Warrior")
        .withCardInLibrary(1, "Plains")
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(2, "Island")
        .withActivePlayer(1)
        .withPriorityPlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun ScenarioTestBase.TestGame.cast(x: Int, vararg names: String) = execute(
        CastSpell(
            player1Id,
            state.getHand(player1Id).single(),
            names.map { ChosenTarget.Permanent(findPermanent(it)!!) },
            x
        )
    )

    private fun ScenarioTestBase.TestGame.tapped(name: String) =
        state.getEntity(findPermanent(name)!!)!!.has<TappedComponent>()

    init {
        test("X=2 taps both targets, then scries 1") {
            val game = board()
            val top = game.state.getLibrary(game.player1Id).first()

            game.cast(2, "Grizzly Bears", "Hill Giant").error shouldBe null
            game.resolveStack()

            val scry = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            withClue("scry 1 looks at the top card") { scry.options shouldBe listOf(top) }
            game.selectCards(listOf(top)).error shouldBe null
            game.resolveStack()

            withClue("both targets tapped") {
                game.tapped("Grizzly Bears") shouldBe true
                game.tapped("Hill Giant") shouldBe true
            }
            withClue("untargeted creature untouched") { game.tapped("Minotaur Warrior") shouldBe false }
            withClue("scried card went to the bottom") {
                game.state.getLibrary(game.player1Id).last() shouldBe top
            }
        }

        test("target count must equal X") {
            val game = board()
            game.cast(2, "Grizzly Bears").error shouldNotBe null
        }
    }
}
