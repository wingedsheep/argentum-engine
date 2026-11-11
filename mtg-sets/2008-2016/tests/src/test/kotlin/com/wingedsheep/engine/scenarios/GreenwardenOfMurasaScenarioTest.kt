package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Greenwarden of Murasa — the enters trigger returns a graveyard card; the dies trigger returns one
 * only if you exile Greenwarden itself.
 */
class GreenwardenOfMurasaScenarioTest : ScenarioTestBase() {
    init {
        test("entering returns target card from your graveyard to your hand") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Greenwarden of Murasa")
                .withLandsOnBattlefield(1, "Forest", 6)
                .withCardInGraveyard(1, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bears = game.findCardsInGraveyard(1, "Grizzly Bears").first()

            game.castSpell(1, "Greenwarden of Murasa").error shouldBe null
            game.resolveStack()
            game.selectTargets(listOf(bears)).error shouldBe null
            game.resolveStack()
            game.answerYesNo(true)
            game.resolveStack()

            game.isInHand(1, "Grizzly Bears") shouldBe true
        }

        test("dying: exiling it returns target card to your hand") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Greenwarden of Murasa")
                .withCardInGraveyard(1, "Grizzly Bears")
                .withCardInHand(1, "Go for the Throat")
                .withLandsOnBattlefield(1, "Swamp", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val warden = game.findPermanent("Greenwarden of Murasa")!!

            game.castSpell(1, "Go for the Throat", warden).error shouldBe null
            game.resolveStack()
            val bears = game.findCardsInGraveyard(1, "Grizzly Bears").first()
            game.selectTargets(listOf(bears)).error shouldBe null
            game.resolveStack()
            game.answerYesNo(true)
            game.resolveStack()

            withClue("Greenwarden was exiled as the cost of the return") {
                game.isInExile(1, "Greenwarden of Murasa") shouldBe true
            }
            game.isInHand(1, "Grizzly Bears") shouldBe true
        }

        test("dying: targeting itself exiles it and returns nothing") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Greenwarden of Murasa")
                .withCardInHand(1, "Go for the Throat")
                .withLandsOnBattlefield(1, "Swamp", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val warden = game.findPermanent("Greenwarden of Murasa")!!

            game.castSpell(1, "Go for the Throat", warden).error shouldBe null
            game.resolveStack()
            val self = game.findCardsInGraveyard(1, "Greenwarden of Murasa").first()
            game.selectTargets(listOf(self)).error shouldBe null
            game.resolveStack()
            game.answerYesNo(true)
            game.resolveStack()

            withClue("the exiled Greenwarden is a new object, not the targeted graveyard card") {
                game.isInExile(1, "Greenwarden of Murasa") shouldBe true
                game.isInHand(1, "Greenwarden of Murasa") shouldBe false
            }
        }

        test("dying: declining to exile it returns nothing") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Greenwarden of Murasa")
                .withCardInGraveyard(1, "Grizzly Bears")
                .withCardInHand(1, "Go for the Throat")
                .withLandsOnBattlefield(1, "Swamp", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val warden = game.findPermanent("Greenwarden of Murasa")!!

            game.castSpell(1, "Go for the Throat", warden).error shouldBe null
            game.resolveStack()
            val bears = game.findCardsInGraveyard(1, "Grizzly Bears").first()
            game.selectTargets(listOf(bears)).error shouldBe null
            game.resolveStack()
            game.answerYesNo(false)
            game.resolveStack()

            game.isInGraveyard(1, "Greenwarden of Murasa") shouldBe true
            game.isInGraveyard(1, "Grizzly Bears") shouldBe true
        }
    }
}
