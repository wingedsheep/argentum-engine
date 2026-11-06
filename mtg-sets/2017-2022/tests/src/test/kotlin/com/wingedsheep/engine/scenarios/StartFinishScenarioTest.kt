package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Start // Finish (AKH).
 *
 * Start {2}{W}: "Create two 1/1 white Warrior creature tokens with vigilance."
 * Finish {2}{B}, Aftermath: "As an additional cost to cast this spell, sacrifice a creature.
 * Destroy target creature."
 *
 * Finish is the first split half with its own additional cost: the graveyard offer must gate on
 * having a creature to sacrifice and publish the sacrifice picker, and Start must not inherit it.
 */
class StartFinishScenarioTest : ScenarioTestBase() {

    init {
        fun game(startInHand: Boolean) = scenario()
            .withPlayers("Caster", "Opponent")
            .withLandsOnBattlefield(1, "Plains", 3)
            .withLandsOnBattlefield(1, "Swamp", 3)
            .apply {
                if (startInHand) withCardInHand(1, "Start // Finish") else withCardInGraveyard(1, "Start // Finish")
            }
            .withCardOnBattlefield(2, "Hill Giant")
            .withCardInLibrary(1, "Plains")
            .withCardInLibrary(2, "Plains")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        test("Start makes two Warriors with no extra cost, then Finish sacrifices one to destroy a creature") {
            val game = game(startInHand = true)
            val inHand = game.findCardsInHand(1, "Start // Finish").single()

            val startOffer = game.getLegalActions(1).single {
                (it.action as? CastSpell)?.let { cs -> cs.cardId == inHand && cs.faceIndex == 0 } == true
            }
            withClue("Start carries no sacrifice — Finish's cost belongs to Finish alone") {
                startOffer.additionalCostInfo.shouldBeNull()
            }
            game.execute(CastSpell(game.player1Id, inHand, faceIndex = 0)).error.shouldBeNull()
            game.resolveStack()

            val warriors = game.findPermanents("Warrior Token")
            warriors shouldHaveSize 2
            game.isInGraveyard(1, "Start // Finish") shouldBe true

            val inGraveyard = game.findCardsInGraveyard(1, "Start // Finish").single()
            val finishOffer = game.getLegalActions(1).single {
                (it.action as? CastSpell)?.let { cs -> cs.cardId == inGraveyard && cs.faceIndex == 1 } == true
            }
            withClue("the aftermath offer publishes the sacrifice picker over the caster's creatures") {
                finishOffer.additionalCostInfo.shouldNotBeNull().costType shouldBe "SacrificePermanent"
                finishOffer.additionalCostInfo!!.validSacrificeTargets shouldContainExactlyInAnyOrder warriors
            }

            val giant = game.findPermanent("Hill Giant").shouldNotBeNull()
            withClue("the sacrifice is mandatory — Finish without it is rejected") {
                game.execute(
                    CastSpell(game.player1Id, inGraveyard, listOf(ChosenTarget.Permanent(giant)), faceIndex = 1)
                ).error shouldNotBe null
            }
            game.execute(
                CastSpell(
                    game.player1Id, inGraveyard, listOf(ChosenTarget.Permanent(giant)), faceIndex = 1,
                    additionalCostPayment = AdditionalCostPayment(sacrificedPermanents = listOf(warriors.first())),
                )
            ).error.shouldBeNull()
            game.resolveStack()

            game.findPermanent("Hill Giant").shouldBeNull()
            game.findPermanents("Warrior Token") shouldHaveSize 1
            withClue("Finish, cast from the graveyard, is exiled") {
                game.isInExile(1, "Start // Finish") shouldBe true
            }
        }

        test("with no creature to sacrifice, Finish is not offered from the graveyard") {
            val game = game(startInHand = false)
            val inGraveyard = game.findCardsInGraveyard(1, "Start // Finish").single()
            game.getLegalActions(1).none {
                (it.action as? CastSpell)?.cardId == inGraveyard
            } shouldBe true
        }
    }
}
