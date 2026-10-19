package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Junji, the Midnight Sky (NEO #102) — "When Junji dies, choose one — • Each opponent discards two
 * cards and loses 2 life. • Put target non-Dragon creature card from a graveyard onto the battlefield
 * under your control. You lose 2 life."
 *
 * Covers the reanimation mode: it reaches into an *opponent's* graveyard, the card enters under
 * Junji's controller, the controller pays 2 life, and Dragon cards (Junji itself included) are not
 * legal targets.
 */
class JunjiTheMidnightSkyScenarioTest : ScenarioTestBase() {

    /** Alice's Junji dies to her own Murder; Bob's graveyard holds a Grizzly Bears and a Shivan Dragon. */
    private fun killJunji(): TestGame {
        val game = scenario()
            .withPlayers("Alice", "Bob")
            .withCardOnBattlefield(1, "Junji, the Midnight Sky")
            .withCardInHand(1, "Murder")
            .withLandsOnBattlefield(1, "Swamp", 3)
            .withCardInLibrary(1, "Swamp")
            .withCardInLibrary(2, "Swamp")
            .withCardInGraveyard(2, "Grizzly Bears")
            .withCardInGraveyard(2, "Shivan Dragon")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        game.castSpell(1, "Murder", game.findPermanent("Junji, the Midnight Sky")!!).error shouldBe null
        game.resolveStack()
        game.isInGraveyard(1, "Junji, the Midnight Sky") shouldBe true
        return game
    }

    private fun TestGame.chooseMode(label: String) {
        val decision = getPendingDecision().shouldBeInstanceOf<ChooseOptionDecision>()
        val index = decision.options.indexOfFirst { it.contains(label, ignoreCase = true) }
        check(index >= 0) { "$label not offered; options=${decision.options}" }
        submitDecision(OptionChosenResponse(decision.id, index)).error shouldBe null
    }

    init {
        context("Junji, the Midnight Sky") {

            test("reanimates a non-Dragon creature from an opponent's graveyard under your control") {
                val game = killJunji()
                val bears = game.findCardsInGraveyard(2, "Grizzly Bears").single()
                val dragon = game.findCardsInGraveyard(2, "Shivan Dragon").single()
                val junji = game.findCardsInGraveyard(1, "Junji, the Midnight Sky").single()

                game.chooseMode("non-Dragon creature card")
                val targets = game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
                val legal = targets.legalTargets.values.flatten()
                withClue("Dragon cards, Junji included, are not legal targets") {
                    legal shouldContain bears
                    legal shouldNotContain dragon
                    legal shouldNotContain junji
                }
                game.selectTargets(listOf(bears)).error shouldBe null
                game.resolveStack()

                withClue("the Bears entered the battlefield under Alice's control") {
                    game.state.projectedState.getController(bears) shouldBe game.player1Id
                    game.isInGraveyard(2, "Grizzly Bears") shouldBe false
                }
                withClue("Alice lost 2 life; Bob is untouched") {
                    game.getLifeTotal(1) shouldBe 18
                    game.getLifeTotal(2) shouldBe 20
                }
            }
        }
    }
}
