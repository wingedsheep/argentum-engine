package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Crackle with Power (STX #95) — {X}{X}{X}{R}{R}: "deals five times X damage to each of up to X
 * targets." X is announced before targets (CR 601.2b–c), so it caps a target list that may mix
 * players and creatures; fewer is fine ("up to"), more is not, and X = 0 is a legal cast with no
 * targets.
 */
class CrackleWithPowerScenarioTest : ScenarioTestBase() {

    private fun game(mountains: Int) = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, "Crackle with Power")
        .withLandsOnBattlefield(1, "Mountain", mountains)
        .withCardOnBattlefield(2, "Hill Giant")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.crackle(): EntityId =
        state.getHand(player1Id).first { state.getEntity(it)?.get<CardComponent>()?.name == "Crackle with Power" }

    init {
        context("Crackle with Power") {

            test("X = 2 deals 10 to each of a player and a creature") {
                val game = game(mountains = 8)
                val giant = game.findPermanent("Hill Giant").shouldNotBeNull()
                val cast = game.execute(
                    CastSpell(game.player1Id, game.crackle(),
                        listOf(ChosenTarget.Player(game.player2Id), ChosenTarget.Permanent(giant)), xValue = 2)
                )
                withClue("cast: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()
                game.getLifeTotal(2) shouldBe 10
                game.isOnBattlefield("Hill Giant") shouldBe false
            }

            test("up to X: X = 2 with a single target is legal") {
                val game = game(mountains = 8)
                val cast = game.execute(
                    CastSpell(game.player1Id, game.crackle(), listOf(ChosenTarget.Player(game.player2Id)), xValue = 2)
                )
                cast.error shouldBe null
                game.resolveStack()
                game.getLifeTotal(2) shouldBe 10
            }

            test("more targets than X is rejected") {
                val game = game(mountains = 5)
                val giant = game.findPermanent("Hill Giant").shouldNotBeNull()
                val cast = game.execute(
                    CastSpell(game.player1Id, game.crackle(),
                        listOf(ChosenTarget.Player(game.player2Id), ChosenTarget.Permanent(giant)), xValue = 1)
                )
                cast.error shouldNotBe null
                game.isInHand(1, "Crackle with Power") shouldBe true
            }

            test("the same target can't fill two of the X slots (CR 115.3)") {
                val game = game(mountains = 8)
                val cast = game.execute(
                    CastSpell(game.player1Id, game.crackle(),
                        listOf(ChosenTarget.Player(game.player2Id), ChosenTarget.Player(game.player2Id)), xValue = 2)
                )
                cast.error shouldNotBe null
                game.getLifeTotal(2) shouldBe 20
            }

            test("X = 0 casts with no targets and does nothing") {
                val game = game(mountains = 2)
                game.execute(CastSpell(game.player1Id, game.crackle(), emptyList(), xValue = 0)).error shouldBe null
                game.resolveStack()
                game.getLifeTotal(2) shouldBe 20
                game.isInGraveyard(1, "Crackle with Power") shouldBe true
            }

            test("the offer tells the client the target count follows X") {
                val game = game(mountains = 5)
                val offer = game.getLegalActions(1)
                    .first { it.actionType == "CastSpell" && it.description.contains("Crackle with Power") }
                offer.xConstrainsTargetCount shouldBe true
                offer.xConstrainsTargetCountExactly shouldBe false
                offer.minTargets shouldBe 0
                offer.validTargets.shouldNotBeNull().contains(game.player2Id) shouldBe true
            }
        }
    }
}
