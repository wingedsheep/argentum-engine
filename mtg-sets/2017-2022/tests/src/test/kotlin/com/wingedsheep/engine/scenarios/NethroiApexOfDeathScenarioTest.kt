package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Nethroi, Apex of Death (IKO #197) — "Whenever this creature mutates, return any number of target
 * creature cards with total power 10 or less from your graveyard to the battlefield."
 *
 * The cap is on the summed power of the chosen cards (`TotalCap.power`): checked when the targets
 * are chosen, using each card's power as it is in the graveyard — a `*` power card's
 * characteristic-defining ability included — and re-checked on resolution, where a set that has
 * grown past 10 makes every target illegal (rulings).
 */
class NethroiApexOfDeathScenarioTest : ScenarioTestBase() {

    private fun ScenarioBuilder.withNethroiReady() = this
        .withPlayers("P1", "P2")
        .withCardInHand(1, "Nethroi, Apex of Death")
        .withLandsOnBattlefield(1, "Swamp", 4)
        .withLandsOnBattlefield(1, "Forest", 3)
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

    /** Mutate Nethroi over P1's Grizzly Bears; leaves the mutate trigger's target choice pending. */
    private fun TestGame.mutateOverBears(): ChooseTargetsDecision {
        val bears = findPermanents("Grizzly Bears").single { state.projectedState.getController(it) == player1Id }
        val offer = getLegalActions(1).single { (it.action as? CastSpell)?.alternativeCostType == AlternativeCostType.MUTATE }
        execute((offer.action as CastSpell).copy(targets = listOf(ChosenTarget.Permanent(bears)))).error shouldBe null
        resolveStack()
        answerYesNo(true)
        return getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
    }

    private fun TestGame.graveyardCard(name: String) = findCardsInGraveyard(1, name).single()

    init {
        context("Nethroi, Apex of Death") {

            test("returns any number of creature cards whose total power is 10 or less") {
                val game = scenario().withNethroiReady()
                    .withCardInGraveyard(1, "Craw Wurm")
                    .withCardInGraveyard(1, "Hill Giant")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInGraveyard(1, "Savannah Lions")
                    .withCardInGraveyard(1, "Ornithopter")
                    .withCardInGraveyard(1, "Ornithopter")
                    .build()
                val wurm = game.graveyardCard("Craw Wurm")
                val giant = game.graveyardCard("Hill Giant")
                val graveBears = game.graveyardCard("Grizzly Bears")
                val lions = game.graveyardCard("Savannah Lions")
                val thopters = game.findCardsInGraveyard(1, "Ornithopter")

                val targets = game.mutateOverBears()
                withClue("the decision carries the cap and each candidate's power") {
                    val req = targets.targetRequirements.single()
                    req.totalCap shouldBe 10
                    req.totalCapLabel shouldBe "power"
                    req.targetValues?.get(wurm) shouldBe 6
                    req.targetValues?.get(thopters.first()) shouldBe 0
                }
                withClue("Craw Wurm + Hill Giant + Grizzly Bears is total power 11") {
                    game.selectTargets(listOf(wurm, giant, graveBears)).error.shouldNotBeNull()
                }
                withClue("power-0 cards ride along with any selection: 6 + 3 + 1 + 0 + 0 = 10") {
                    game.selectTargets(listOf(wurm, giant, lions, thopters[0], thopters[1])).error shouldBe null
                }
                game.resolveStack()

                withClue("the chosen cards return; the graveyard Grizzly Bears stays") {
                    game.isOnBattlefield("Craw Wurm") shouldBe true
                    game.isOnBattlefield("Hill Giant") shouldBe true
                    game.isOnBattlefield("Savannah Lions") shouldBe true
                    game.findPermanents("Ornithopter").size shouldBe 2
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                }
            }

            test("a * power card counts the power its characteristic-defining ability gives it in the graveyard") {
                val game = scenario().withNethroiReady()
                    .withCardInGraveyard(1, "Craw Wurm")
                    .withCardInGraveyard(1, "Lhurgoyf")
                    .withCardInGraveyard(1, "Hill Giant")
                    .build()
                val wurm = game.graveyardCard("Craw Wurm")
                val lhurgoyf = game.graveyardCard("Lhurgoyf")
                val giant = game.graveyardCard("Hill Giant")

                val targets = game.mutateOverBears()
                withClue("Lhurgoyf counts the three creature cards in graveyards, itself included") {
                    targets.targetRequirements.single().targetValues?.get(lhurgoyf) shouldBe 3
                }
                withClue("Craw Wurm + Lhurgoyf + Hill Giant is 6 + 3 + 3 = 12") {
                    game.selectTargets(listOf(wurm, lhurgoyf, giant)).error.shouldNotBeNull()
                }
                game.selectTargets(listOf(wurm, lhurgoyf)).error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Craw Wurm") shouldBe true
                game.isOnBattlefield("Lhurgoyf") shouldBe true
                game.isInGraveyard(1, "Hill Giant") shouldBe true
            }

            test("if the targets' total power grows past 10 before resolution, nothing is returned") {
                val game = scenario().withNethroiReady()
                    .withCardInGraveyard(1, "Craw Wurm")
                    .withCardInGraveyard(1, "Lhurgoyf")
                    .withCardInGraveyard(1, "Hill Giant")
                    .withCardInGraveyard(1, "Savannah Lions")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardInHand(2, "Shock")
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .build()
                val wurm = game.graveyardCard("Craw Wurm")
                val lhurgoyf = game.graveyardCard("Lhurgoyf")

                game.mutateOverBears()
                withClue("Craw Wurm (6) + Lhurgoyf (4 creature cards in graveyards) is exactly 10") {
                    game.selectTargets(listOf(wurm, lhurgoyf)).error shouldBe null
                }
                // P2 Shocks their own Grizzly Bears in response: a fifth creature card makes Lhurgoyf 5.
                game.passPriority()
                val p2Bears = game.findPermanents("Grizzly Bears").single {
                    game.state.projectedState.getController(it) == game.player2Id
                }
                game.castSpell(2, "Shock", p2Bears).error shouldBe null
                game.resolveStack()

                withClue("6 + 5 = 11 is over the cap, so every target is illegal and both stay put") {
                    game.isInGraveyard(1, "Craw Wurm") shouldBe true
                    game.isInGraveyard(1, "Lhurgoyf") shouldBe true
                    game.isOnBattlefield("Craw Wurm") shouldBe false
                }
            }

            test("cast normally it doesn't mutate, so nothing returns") {
                val game = scenario().withNethroiReady()
                    .withCardInGraveyard(1, "Hill Giant")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .build()
                game.castSpell(1, "Nethroi, Apex of Death").error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Nethroi, Apex of Death") shouldBe true
                game.isInGraveyard(1, "Hill Giant") shouldBe true
            }
        }
    }
}
