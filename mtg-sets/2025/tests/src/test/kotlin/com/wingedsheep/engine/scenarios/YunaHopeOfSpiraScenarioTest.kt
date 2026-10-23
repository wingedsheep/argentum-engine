package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ManaSourcesSelectedResponse
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario tests for Yuna, Hope of Spira (FIN).
 *
 * Clause 1: a conditional ("during your turn") anthem granting trample, lifelink, and ward {2} to
 * Yuna and enchantment creatures you control — asserted via projected keywords on your turn vs. the
 * opponent's turn. Clause 2: an end-step trigger returning an enchantment card from your graveyard
 * with a finality counter.
 */
class YunaHopeOfSpiraScenarioTest : ScenarioTestBase() {

    private val projector = StateProjector()

    init {
        context("Yuna, Hope of Spira") {

            test("during your turn, Yuna and your enchantment creatures gain trample, lifelink, and ward") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Yuna, Hope of Spira", summoningSickness = false)
                    .withCardOnBattlefield(1, "Enduring Curiosity", summoningSickness = false) // Enchantment Creature
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val yuna = game.findPermanent("Yuna, Hope of Spira")!!
                val enchantmentCreature = game.findPermanent("Enduring Curiosity")!!
                val projected = projector.project(game.state)

                withClue("On your turn, Yuna has trample, lifelink, and ward") {
                    projected.hasKeyword(yuna, Keyword.TRAMPLE) shouldBe true
                    projected.hasKeyword(yuna, Keyword.LIFELINK) shouldBe true
                    projected.hasKeyword(yuna, Keyword.WARD) shouldBe true
                }
                withClue("On your turn, an enchantment creature you control also gains the keywords") {
                    projected.hasKeyword(enchantmentCreature, Keyword.TRAMPLE) shouldBe true
                    projected.hasKeyword(enchantmentCreature, Keyword.LIFELINK) shouldBe true
                }
            }

            test("on your turn, Yuna's own ward {2} triggers and counters unpaid removal") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Yuna, Hope of Spira", summoningSickness = false)
                    .withCardInHand(2, "Murder")
                    .withLandsOnBattlefield(2, "Swamp", 3)
                    .withActivePlayer(1)
                    .withPriorityPlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(2, "Murder", game.findPermanent("Yuna, Hope of Spira")!!).error shouldBe null
                game.resolveStack()

                withClue("ward {2} went unpaid, so Murder was countered") {
                    game.isOnBattlefield("Yuna, Hope of Spira") shouldBe true
                    game.isInGraveyard(2, "Murder") shouldBe true
                }
            }

            test("on your turn, paying Yuna's single ward {2} lets the removal resolve") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Yuna, Hope of Spira", summoningSickness = false)
                    .withCardInHand(2, "Murder")
                    .withLandsOnBattlefield(2, "Swamp", 5)
                    .withActivePlayer(1)
                    .withPriorityPlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(2, "Murder", game.findPermanent("Yuna, Hope of Spira")!!).error shouldBe null
                game.resolveStack()

                val ward = game.getPendingDecision()
                ward.shouldBeInstanceOf<SelectManaSourcesDecision>()
                game.submitDecision(ManaSourcesSelectedResponse(ward.id, autoPay = true)).error shouldBe null
                game.resolveStack()

                withClue("one ward {2} trigger, paid with the last two Swamps, so Murder resolved") {
                    game.getPendingDecision() shouldBe null
                    game.isInGraveyard(1, "Yuna, Hope of Spira") shouldBe true
                }
            }

            test("the anthem is inactive during an opponent's turn") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Yuna, Hope of Spira", summoningSickness = false)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val yuna = game.findPermanent("Yuna, Hope of Spira")!!
                val projected = projector.project(game.state)

                withClue("It's not your turn, so the anthem grants nothing") {
                    projected.hasKeyword(yuna, Keyword.TRAMPLE) shouldBe false
                    projected.hasKeyword(yuna, Keyword.LIFELINK) shouldBe false
                }
            }

            test("at your end step, return an enchantment from your graveyard with a finality counter") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Yuna, Hope of Spira", summoningSickness = false)
                    .withCardInGraveyard(1, "Angelic Shield") // {2}{W} Enchantment, no ETB decision
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                // "Up to one target" — choose the enchantment in the graveyard.
                if (game.state.pendingDecision != null) {
                    val target = game.findCardsInGraveyard(1, "Angelic Shield").first()
                    game.selectTargets(listOf(target))
                    game.resolveStack()
                }

                withClue("The enchantment should have returned to the battlefield") {
                    game.isOnBattlefield("Angelic Shield") shouldBe true
                }
                val returned = game.findPermanent("Angelic Shield")!!
                val counters = game.state.getEntity(returned)?.get<CountersComponent>()
                withClue("It returns with a finality counter on it") {
                    (counters?.getCount(CounterType.FINALITY) ?: 0) shouldBe 1
                }
            }
        }
    }
}
