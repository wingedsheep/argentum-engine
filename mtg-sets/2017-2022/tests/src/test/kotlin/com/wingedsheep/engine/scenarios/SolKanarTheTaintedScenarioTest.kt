package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Sol'Kanar the Tainted (DMU #219) — "At the beginning of your end step, choose one that hasn't
 * been chosen — draw / drain 2 / 3 damage to up to one other creature or planeswalker / exile
 * Sol'Kanar, then return it to the battlefield under an opponent's control."
 *
 * The memory of chosen modes belongs to the object: the last mode hands Sol'Kanar to the opponent
 * as a *new* object, so (per the ruling) it triggers on that player's end step with every mode on
 * offer again, and that player makes the choice.
 */
class SolKanarTheTaintedScenarioTest : ScenarioTestBase() {

    private val sol = "Sol'Kanar the Tainted"
    private val draw = "Draw a card"
    private val drain = "Each opponent loses 2 life and you gain 2 life"
    private val burn = "Sol'Kanar deals 3 damage to up to one other target creature or planeswalker"
    private val handOff = "Exile Sol'Kanar, then return it to the battlefield under an opponent's control"

    private fun TestGame.resolveToModeChoice(): ChooseOptionDecision {
        resolveStack()
        val decision = getPendingDecision()
        decision.shouldNotBeNull()
        return decision as ChooseOptionDecision
    }

    private fun TestGame.chooseMode(decision: ChooseOptionDecision, description: String) {
        val index = decision.options.indexOf(description)
        check(index >= 0) { "Mode '$description' not offered; options=${decision.options}" }
        submitDecision(OptionChosenResponse(decision.id, index)).error shouldBe null
    }

    private fun board(): TestGame {
        var builder = scenario()
            .withPlayers("Player1", "Player2")
            .withCardOnBattlefield(1, sol)
            // A legal target for the "up to one other target" mode, so its offer never hinges on emptiness.
            .withCardOnBattlefield(2, "Grizzly Bears")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        repeat(10) { builder = builder.withCardInLibrary(1, "Swamp") }
        repeat(10) { builder = builder.withCardInLibrary(2, "Forest") }
        return builder.build()
    }

    init {
        test("a chosen mode is not offered again on the next end step") {
            val game = board()

            game.passUntilPhase(Phase.ENDING, Step.END)
            val first = game.resolveToModeChoice()
            first.options shouldContainExactlyInAnyOrder listOf(draw, drain, burn, handOff)
            game.chooseMode(first, drain)
            game.resolveStack()

            game.getLifeTotal(1) shouldBe 22
            game.getLifeTotal(2) shouldBe 18

            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)   // opponent's turn
            game.passUntilPhase(Phase.ENDING, Step.END)          // opponent's end step: no trigger
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)   // our turn
            game.passUntilPhase(Phase.ENDING, Step.END)
            val second = game.resolveToModeChoice()
            withClue("the drain mode is spent") {
                second.options shouldNotContain drain
                second.options shouldContain draw
            }
        }

        test("the last mode hands Sol'Kanar to the opponent as a new object with every mode fresh") {
            val game = board()

            game.passUntilPhase(Phase.ENDING, Step.END)
            game.chooseMode(game.resolveToModeChoice(), handOff)
            game.resolveStack()

            val solId = game.findPermanent(sol)!!
            withClue("the opponent controls Sol'Kanar now") {
                game.state.projectedState.getController(solId) shouldBe game.player2Id
            }

            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)   // opponent's turn
            game.passUntilPhase(Phase.ENDING, Step.END)
            val theirs = game.resolveToModeChoice()
            withClue("it triggers on the opponent's end step; they choose, from all four modes") {
                theirs.playerId shouldBe game.player2Id
                theirs.options shouldContainExactlyInAnyOrder listOf(draw, drain, burn, handOff)
            }
            game.chooseMode(theirs, drain)
            game.resolveStack()
            game.getLifeTotal(2) shouldBe 22
            game.getLifeTotal(1) shouldBe 18
        }
    }
}
