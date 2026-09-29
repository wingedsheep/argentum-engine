package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Compleat Devotion (ONE #7) — {1}{W} Instant.
 *
 * "Target creature you control gets +2/+2 until end of turn. If that creature has toxic, draw a card."
 *
 * Proof card for "has toxic": toxic projects as `TOXIC_<n>`, never a bare `TOXIC`, so the
 * `withKeyword(TOXIC)` check only works because keyword predicates read the numeric form.
 */
class CompleatDevotionScenarioTest : ScenarioTestBase() {

    init {
        test("on a creature with toxic: +2/+2 and a card") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Crawling Chorus")
                .withCardInHand(1, "Compleat Devotion")
                .withCardInLibrary(1, "Plains")
                .withLandsOnBattlefield(1, "Plains", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val chorus = game.findPermanent("Crawling Chorus")!!
            val handBefore = game.handSize(1)
            game.castSpell(1, "Compleat Devotion", chorus).error shouldBe null
            game.resolveStack()

            game.state.projectedState.getPower(chorus) shouldBe 3
            game.state.projectedState.getToughness(chorus) shouldBe 3
            withClue("the spell left hand (-1) and a card was drawn (+1)") {
                game.handSize(1) shouldBe handBefore
            }
        }

        test("on a creature without toxic: +2/+2 and no card") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(1, "Compleat Devotion")
                .withCardInLibrary(1, "Plains")
                .withLandsOnBattlefield(1, "Plains", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            val handBefore = game.handSize(1)
            game.castSpell(1, "Compleat Devotion", bears).error shouldBe null
            game.resolveStack()

            game.state.projectedState.getPower(bears) shouldBe 4
            game.handSize(1) shouldBe handBefore - 1
        }
    }
}
