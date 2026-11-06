package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * By Force (AKH #123) — {X}{R} Sorcery. "Destroy X target artifacts."
 *
 * Pins that the target count is tied to X (exactly X targets) and that every target is destroyed.
 */
class ByForceScenarioTest : ScenarioTestBase() {

    init {
        context("By Force") {

            fun TestGame.byForce(x: Int, targets: List<ChosenTarget>) = execute(
                CastSpell(
                    playerId = player1Id,
                    cardId = state.getHand(player1Id).first { id ->
                        state.getEntity(id)?.get<CardComponent>()?.name == "By Force"
                    },
                    targets = targets,
                    xValue = x
                )
            )

            fun board() = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardInHand(1, "By Force")
                .withCardOnBattlefield(2, "Ornithopter")
                .withCardOnBattlefield(2, "Millstone")
                .withCardOnBattlefield(2, "Jalum Tome")
                .withLandsOnBattlefield(1, "Mountain", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

            test("X=2 destroys both targeted artifacts and leaves the untargeted one") {
                val game = board().build()
                val ornithopter = game.findPermanent("Ornithopter")!!
                val millstone = game.findPermanent("Millstone")!!

                val cast = game.byForce(2, listOf(ChosenTarget.Permanent(ornithopter), ChosenTarget.Permanent(millstone)))
                withClue("{2}{R} is payable: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                game.isInGraveyard(2, "Ornithopter") shouldBe true
                game.isInGraveyard(2, "Millstone") shouldBe true
                game.isOnBattlefield("Jalum Tome") shouldBe true
            }

            test("the number of targets must equal X") {
                val game = board().build()
                val ornithopter = game.findPermanent("Ornithopter")!!

                val cast = game.byForce(2, listOf(ChosenTarget.Permanent(ornithopter)))
                withClue("one target for X=2 is illegal") { cast.error.shouldNotBeNull() }
                game.isOnBattlefield("Ornithopter") shouldBe true
            }
        }
    }
}
