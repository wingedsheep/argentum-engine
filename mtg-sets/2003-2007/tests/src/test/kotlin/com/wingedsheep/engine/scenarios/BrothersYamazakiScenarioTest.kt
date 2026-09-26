package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Brothers Yamazaki (CHK #160a) — "If there are exactly two permanents named Brothers Yamazaki on
 * the battlefield, the 'legend rule' doesn't apply to them. Each other creature named Brothers
 * Yamazaki gets +2/+2 and has haste."
 */
class BrothersYamazakiScenarioTest : ScenarioTestBase() {

    init {
        context("Brothers Yamazaki") {

            test("casting the second copy keeps both; each pumps the other") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Brothers Yamazaki")
                    .withCardInHand(1, "Brothers Yamazaki")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Brothers Yamazaki").error shouldBe null
                game.resolveStack()

                withClue("no legend-rule choice with exactly two") {
                    game.state.pendingDecision shouldBe null
                }
                val brothers = game.findPermanents("Brothers Yamazaki")
                brothers.size shouldBe 2
                val projected = game.state.projectedState
                for (b in brothers) {
                    projected.getPower(b) shouldBe 4
                    projected.getToughness(b) shouldBe 3
                    projected.hasKeyword(b, Keyword.HASTE) shouldBe true
                }
            }

            test("a third copy switches the exemption off: the legend rule leaves one") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Brothers Yamazaki")
                    .withCardOnBattlefield(1, "Brothers Yamazaki")
                    .withCardInHand(1, "Brothers Yamazaki")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Brothers Yamazaki").error shouldBe null
                game.resolveStack()

                val decision = game.state.pendingDecision
                decision.shouldBeInstanceOf<SelectCardsDecision>()
                decision.options.size shouldBe 3
                game.selectCards(listOf(decision.options.first()))

                val left = game.findPermanents("Brothers Yamazaki")
                left.size shouldBe 1
                withClue("alone, it gets no bonus from itself") {
                    game.state.projectedState.getPower(left.single()) shouldBe 2
                }
            }

            test("a lone Brothers Yamazaki pumps an opponent's copy") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Brothers Yamazaki")
                    .withCardOnBattlefield(2, "Brothers Yamazaki")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val projected = game.state.projectedState
                for (b in game.findPermanents("Brothers Yamazaki")) {
                    projected.getPower(b) shouldBe 4
                }
            }
        }
    }
}
