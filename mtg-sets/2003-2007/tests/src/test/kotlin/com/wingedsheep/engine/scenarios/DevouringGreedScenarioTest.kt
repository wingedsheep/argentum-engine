package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Devouring Greed (CHK #110) — "As an additional cost to cast this spell, you may sacrifice any
 * number of Spirits. Target player loses 2 life plus 2 life for each Spirit sacrificed this way. You
 * gain that much life."
 */
class DevouringGreedScenarioTest : ScenarioTestBase() {

    private fun TestGame.castGreed(sacrificed: List<EntityId>?) = execute(
        CastSpell(
            playerId = player1Id,
            cardId = findCardsInHand(1, "Devouring Greed").single(),
            targets = listOf(ChosenTarget.Player(player2Id)),
            additionalCostPayment = sacrificed?.let { AdditionalCostPayment(variableCostPermanents = it) },
        )
    )

    private fun board() = scenario()
        .withPlayers("Alice", "Bob")
        .withCardInHand(1, "Devouring Greed")
        .withLandsOnBattlefield(1, "Swamp", 4)
        .withCardOnBattlefield(1, "Kami of Old Stone")
        .withCardOnBattlefield(1, "Kami of Ancient Law")
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        context("Devouring Greed") {

            test("offers the Spirits you control as an optional sacrifice picker") {
                val game = board()
                val cast = game.getLegalActions(1).single { it.description == "Cast Devouring Greed" }
                val info = cast.additionalCostInfo.shouldNotBeNull()
                info.costType shouldBe "SacrificeVariable"
                info.sacrificeCount shouldBe 0
                info.validSacrificeTargets shouldContainExactlyInAnyOrder listOf(
                    game.findPermanent("Kami of Old Stone")!!,
                    game.findPermanent("Kami of Ancient Law")!!,
                )
            }

            test("each sacrificed Spirit drains 2 more") {
                val game = board()
                val spirits = listOf(game.findPermanent("Kami of Old Stone")!!, game.findPermanent("Kami of Ancient Law")!!)

                game.castGreed(spirits).error shouldBe null
                withClue("the Spirits are sacrificed as the spell is cast") {
                    game.isInGraveyard(1, "Kami of Old Stone") shouldBe true
                    game.isInGraveyard(1, "Kami of Ancient Law") shouldBe true
                }
                game.resolveStack()

                game.getLifeTotal(2) shouldBe 14
                game.getLifeTotal(1) shouldBe 26
            }

            test("sacrificing nothing still drains 2") {
                val game = board()
                game.castGreed(null).error shouldBe null
                game.resolveStack()

                game.getLifeTotal(2) shouldBe 18
                game.getLifeTotal(1) shouldBe 22
                game.findPermanent("Kami of Old Stone").shouldNotBeNull()
            }

            test("a non-Spirit can't be sacrificed to it") {
                val game = board()
                game.castGreed(listOf(game.findPermanent("Grizzly Bears")!!)).error shouldNotBe null
                game.findPermanent("Grizzly Bears").shouldNotBeNull()
            }
        }
    }
}
