package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Peregrine Drake (USG #88) — "When this creature enters, untap up to five lands." The Oracle text
 * has no "target": the lands are chosen as the trigger resolves and may be any lands, so an
 * opponent's hexproof land is a legal pick. Palinchron, Great Whale and Cloud of Faeries share the
 * same script shape with a different count.
 */
class PeregrineDrakeScenarioTest : ScenarioTestBase() {
    init {
        test("untaps up to five lands chosen on resolution, including an opponent's hexproof land") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardInHand(1, "Peregrine Drake")
                .withLandsOnBattlefield(1, "Island", 5)
                .withCardOnBattlefield(2, "Valgavoth's Lair", tapped = true)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Peregrine Drake").error shouldBe null
            game.resolveStack()

            val islands = game.findPermanents("Island")
            val lair = game.findPermanent("Valgavoth's Lair").shouldNotBeNull()
            withClue("all five Islands paid for the Drake") {
                islands.count { game.state.getEntity(it)!!.has<TappedComponent>() } shouldBe 5
            }

            val decision = game.state.pendingDecision as? SelectCardsDecision
            withClue("the choice is made on resolution, as a selection rather than a target") {
                decision.shouldNotBeNull()
                decision.maxSelections shouldBe 5
                decision.minSelections shouldBe 0
                decision.options shouldContain lair
            }
            val picked = listOf(lair) + islands.take(4)
            game.selectCards(picked)

            withClue("the hexproof land and four Islands untap; one Island stays tapped") {
                picked.count { !game.state.getEntity(it)!!.has<TappedComponent>() } shouldBe 5
                islands.count { game.state.getEntity(it)!!.has<TappedComponent>() } shouldBe 1
            }
        }
    }
}
