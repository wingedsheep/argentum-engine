package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Indoctrination Attendant (ONE #16) — {3}{W} 3/4 Phyrexian Cleric, toxic 1.
 * "When this creature enters, you may return another permanent you control to its owner's hand.
 *  If you do, create a 1/1 colorless Phyrexian Mite artifact creature token with toxic 1 and
 *  'This token can't block.'"
 */
class IndoctrinationAttendantScenarioTest : ScenarioTestBase() {

    private fun game() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(1, "Indoctrination Attendant")
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withCardOnBattlefield(2, "Hill Giant")
        .withLandsOnBattlefield(1, "Plains", 4)
        .withActivePlayer(1)
        .withPriorityPlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("returning another permanent creates a Phyrexian Mite") {
            val g = game()
            val bears = g.findPermanent("Grizzly Bears")!!
            val giant = g.findPermanent("Hill Giant")!!

            g.castSpell(1, "Indoctrination Attendant").error shouldBe null
            g.resolveStack()

            val decision = g.getPendingDecision()
            decision.shouldBeInstanceOf<SelectCardsDecision>()
            decision.options shouldContain bears
            decision.options shouldNotContain giant
            decision.options shouldNotContain g.findPermanent("Indoctrination Attendant")!!

            g.selectCards(listOf(bears)).error shouldBe null
            g.resolveStack()

            g.isOnBattlefield("Grizzly Bears") shouldBe false
            g.isInHand(1, "Grizzly Bears") shouldBe true

            val mites = g.findAllPermanents("Phyrexian Mite")
            mites shouldHaveSize 1
            val projected = g.state.projectedState
            projected.getController(mites.single()) shouldBe g.player1Id
            projected.hasKeyword(mites.single(), Keyword.TOXIC) shouldBe true

            val attendant = g.findPermanent("Indoctrination Attendant")!!
            projected.hasKeyword(attendant, Keyword.TOXIC) shouldBe true
        }

        test("declining returns nothing and creates no Mite") {
            val g = game()

            g.castSpell(1, "Indoctrination Attendant").error shouldBe null
            g.resolveStack()

            g.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            g.skipSelection().error shouldBe null
            g.resolveStack()

            g.isOnBattlefield("Grizzly Bears") shouldBe true
            g.findAllPermanents("Phyrexian Mite").shouldBeEmpty()
        }
    }
}
