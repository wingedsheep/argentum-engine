package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Bile Blight — {B}{B} Instant — "Target creature and all other creatures with the same name as that
 * creature get -3/-3 until end of turn."
 *
 * The shrink reaches every creature sharing the target's name, whoever controls it, applies to each
 * exactly once (the target is not hit twice), and leaves differently named creatures alone.
 */
class BileBlightScenarioTest : ScenarioTestBase() {

    init {
        test("target and every same-named creature get -3/-3; others are untouched") {
            val g = scenario()
                .withPlayers()
                .withCardInHand(1, "Bile Blight")
                .withLandsOnBattlefield(1, "Swamp", 2)
                .withCardOnBattlefield(1, "Craw Wurm")
                .withCardOnBattlefield(2, "Craw Wurm")
                .withCardOnBattlefield(2, "Craw Wurm")
                .withCardOnBattlefield(2, "Hill Giant")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .build()

            val wurms = g.findAllPermanents("Craw Wurm")
            wurms shouldHaveSize 3
            val giant = g.findPermanent("Hill Giant")!!
            val target = wurms.first { g.state.projectedState.getController(it) == g.player2Id }

            g.castSpell(1, "Bile Blight", target).error shouldBe null
            g.resolveStack()

            for (wurm in wurms) {
                g.state.projectedState.getPower(wurm) shouldBe 3
                g.state.projectedState.getToughness(wurm) shouldBe 1
            }
            g.state.projectedState.getPower(giant) shouldBe 3
            g.state.projectedState.getToughness(giant) shouldBe 3
        }

        test("a lone target dies to -3/-3") {
            val g = scenario()
                .withPlayers()
                .withCardInHand(1, "Bile Blight")
                .withLandsOnBattlefield(1, "Swamp", 2)
                .withCardOnBattlefield(2, "Hill Giant")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .build()

            val giant = g.findPermanent("Hill Giant")!!
            g.castSpell(1, "Bile Blight", giant).error shouldBe null
            g.resolveStack()

            g.isInGraveyard(2, "Hill Giant") shouldBe true
        }
    }
}
