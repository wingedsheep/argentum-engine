package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Wispweaver Angel (KLD #35, reprinted J22 #266) — {4}{W}{W} Creature — Angel, 4/4.
 *
 *   Flying
 *   When this creature enters, you may exile another target creature you control, then return
 *   that card to the battlefield under its owner's control.
 */
class WispweaverAngelScenarioTest : ScenarioTestBase() {

    private fun angelGame() = scenario()
        .withPlayers("P1", "P2")
        .withCardInHand(1, "Wispweaver Angel")
        .withCardOnBattlefield(1, "Grizzly Bears", tapped = true)
        .withCardOnBattlefield(1, "Hill Giant", tapped = true)
        .withCardOnBattlefield(2, "Savannah Lions")
        .withLandsOnBattlefield(1, "Plains", 6)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        context("Wispweaver Angel") {

            test("enters with flying and may blink another creature you control as a new object") {
                val game = angelGame()
                val bears = game.findPermanent("Grizzly Bears")!!
                val giant = game.findPermanent("Hill Giant")!!

                game.castSpell(1, "Wispweaver Angel").error shouldBe null
                game.resolveStack()

                val angel = game.findPermanent("Wispweaver Angel")!!
                game.state.projectedState.hasKeyword(angel, Keyword.FLYING) shouldBe true

                // The engine asks the "may" first, then the target, for a may-trigger that targets.
                game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
                game.answerYesNo(true).error shouldBe null

                val decision = game.getPendingDecision()
                decision.shouldBeInstanceOf<ChooseTargetsDecision>()
                withClue("only other creatures you control are legal — not the Angel, not an opponent's creature") {
                    decision.legalTargets[0].orEmpty() shouldContainExactlyInAnyOrder listOf(bears, giant)
                }
                game.selectTargets(listOf(bears)).error shouldBe null
                game.resolveStack()

                val returned = game.findPermanent("Grizzly Bears")
                withClue("the exiled creature returns to the battlefield") { returned shouldNotBe null }
                withClue("it returns as a new, untapped object") {
                    game.state.getEntity(returned!!)!!.has<TappedComponent>() shouldBe false
                }
                game.isInExile(1, "Grizzly Bears") shouldBe false
                withClue("the untargeted creature is untouched") {
                    game.state.getEntity(giant)!!.has<TappedComponent>() shouldBe true
                }
            }

            test("declining the may leaves the target where it is") {
                val game = angelGame()
                val bears = game.findPermanent("Grizzly Bears")!!

                game.castSpell(1, "Wispweaver Angel").error shouldBe null
                game.resolveStack()

                game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
                game.answerYesNo(false).error shouldBe null
                game.resolveStack()
                game.hasPendingDecision() shouldBe false

                withClue("the same object stays on the battlefield, still tapped") {
                    game.findPermanent("Grizzly Bears") shouldBe bears
                    game.state.getEntity(bears)!!.has<TappedComponent>() shouldBe true
                }
            }
        }
    }
}
