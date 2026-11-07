package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.ChoiceSlot
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Heartstabber Mosquito (ZEN #97) — {3}{B} 2/2 flying, Kicker {2}{B}.
 * "When this creature enters, if it was kicked, destroy target creature."
 */
class HeartstabberMosquitoScenarioTest : ScenarioTestBase() {
    init {
        test("kicked, its enters trigger destroys target creature") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Heartstabber Mosquito")
                .withLandsOnBattlefield(1, "Swamp", 7)
                .withCardOnBattlefield(2, "Craw Wurm")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val wurm = game.findPermanent("Craw Wurm")!!
            val mosquito = game.findCardsInHand(1, "Heartstabber Mosquito").single()

            game.execute(CastSpell(game.player1Id, mosquito, declaredCostSlot = ChoiceSlot.KICKED))
                .error shouldBe null
            game.resolveStack()

            game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
            game.selectTargets(listOf(wurm)).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(2, "Craw Wurm") shouldBe true
            game.isOnBattlefield("Heartstabber Mosquito") shouldBe true
        }

        test("unkicked, the trigger doesn't happen") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Heartstabber Mosquito")
                .withLandsOnBattlefield(1, "Swamp", 4)
                .withCardOnBattlefield(2, "Craw Wurm")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Heartstabber Mosquito").error shouldBe null
            game.resolveStack()

            game.hasPendingDecision() shouldBe false
            game.isOnBattlefield("Heartstabber Mosquito") shouldBe true
            game.isOnBattlefield("Craw Wurm") shouldBe true
        }
    }
}
