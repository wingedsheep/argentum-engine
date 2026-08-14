package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/** Scenario tests for Stormplain Detainment. */
class StormplainDetainmentScenarioTest : ScenarioTestBase() {

    init {
        context("Stormplain Detainment") {
            // The exile-until-leaves / return-on-LTB seam is exercised end-to-end by
            // BanishingLightTest; here we just confirm Stormplain Detainment exiles an opponent's
            // nonland permanent on ETB and links it for return.
            test("exiles an opponent's creature on ETB") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Stormplain Detainment")
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val victim = game.findPermanent("Hill Giant")!!
                val cast = game.castSpell(1, "Stormplain Detainment")
                withClue("Cast should succeed: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack() // enchantment enters → ETB trigger on stack asks for target

                val selected = game.selectTargets(listOf(victim))
                withClue("ETB target selection should succeed: ${selected.error}") {
                    selected.error shouldBe null
                }
                game.resolveStack()

                withClue("Hill Giant should be exiled while the enchantment is in play") {
                    game.isOnBattlefield("Hill Giant") shouldBe false
                    game.state.getExile(game.player2Id).count {
                        game.state.getEntity(it)?.get<CardComponent>()?.name == "Hill Giant"
                    } shouldBe 1
                }
                withClue("Stormplain Detainment should be on the battlefield holding the exile") {
                    game.isOnBattlefield("Stormplain Detainment") shouldBe true
                }
            }
        }
    }
}
