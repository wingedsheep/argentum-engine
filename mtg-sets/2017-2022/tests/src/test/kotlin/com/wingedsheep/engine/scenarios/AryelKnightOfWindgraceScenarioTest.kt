package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Aryel, Knight of Windgrace (DOM #192):
 * "{B}, {T}, Tap X untapped Knights you control: Destroy target creature with power X or less."
 *
 * Aryel is a Knight, but it pays {T}, so it can never be one of the X Knights — the engine leaves
 * the {T} source out of a tap-X pool in the legal-action listing and rejects it at payment.
 */
class AryelKnightOfWindgraceScenarioTest : ScenarioTestBase() {

    private fun setup() = scenario()
        .withPlayers("Alice", "Bob")
        .withCardOnBattlefield(1, "Aryel, Knight of Windgrace", summoningSickness = false)
        .withCardOnBattlefield(1, "White Knight")
        .withCardOnBattlefield(1, "White Knight")
        .withLandsOnBattlefield(1, "Swamp", 1)
        .withCardOnBattlefield(2, "Hill Giant")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        context("Aryel's tap-X Knights cost") {

            test("the legal action counts only the other untapped Knights") {
                val game = setup()
                val aryel = game.findPermanent("Aryel, Knight of Windgrace")!!
                val knights = game.findPermanents("White Knight")
                val legal = game.getLegalActions(1)
                    .filter { (it.action as? ActivateAbility)?.sourceId == aryel }
                    .single { it.additionalCostInfo?.validTapTargets?.isNotEmpty() == true }

                withClue("max X is the two White Knights, not three") {
                    legal.maxAffordableX shouldBe 2
                }
                legal.additionalCostInfo.shouldNotBeNull().validTapTargets shouldContainExactlyInAnyOrder knights
            }

            test("paying X = 3 with Aryel among the tapped Knights is rejected") {
                val game = setup()
                val aryel = game.findPermanent("Aryel, Knight of Windgrace")!!
                val knights = game.findPermanents("White Knight")
                val giant = game.findPermanent("Hill Giant")!!
                val destroyAbility = cardRegistry.getCard("Aryel, Knight of Windgrace")!!
                    .script.activatedAbilities[1]

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = aryel,
                        abilityId = destroyAbility.id,
                        targets = listOf(ChosenTarget.Permanent(giant)),
                        costPayment = AdditionalCostPayment(tappedPermanents = knights + aryel),
                        xValue = 3
                    )
                )

                result.error shouldNotBe null
                knights.forEach { game.state.getEntity(it)!!.has<TappedComponent>() shouldBe false }
                game.findPermanent("Hill Giant") shouldBe giant
            }

            test("X = 2 paid by the two other Knights taps all three; a power-3 target survives") {
                val game = setup()
                val aryel = game.findPermanent("Aryel, Knight of Windgrace")!!
                val knights = game.findPermanents("White Knight")
                val giant = game.findPermanent("Hill Giant")!!
                val destroyAbility = cardRegistry.getCard("Aryel, Knight of Windgrace")!!
                    .script.activatedAbilities[1]

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = aryel,
                        abilityId = destroyAbility.id,
                        targets = listOf(ChosenTarget.Permanent(giant)),
                        costPayment = AdditionalCostPayment(tappedPermanents = knights),
                        xValue = 2
                    )
                )
                result.error shouldBe null
                (knights + aryel).forEach { game.state.getEntity(it)!!.has<TappedComponent>() shouldBe true }

                game.resolveStack()
                withClue("Hill Giant has power 3 > X = 2, so it survives") {
                    game.findPermanent("Hill Giant") shouldBe giant
                }
            }
        }
    }
}
