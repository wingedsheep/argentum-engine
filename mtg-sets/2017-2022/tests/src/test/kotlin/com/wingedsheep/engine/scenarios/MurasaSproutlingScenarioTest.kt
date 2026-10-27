package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.ChoiceSlot
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Murasa Sproutling (ZNR #196) — Kicker {1}{G}. When this creature enters, if it was kicked, return
 * target card with a kicker ability from your graveyard to your hand.
 */
class MurasaSproutlingScenarioTest : ScenarioTestBase() {

    init {
        test("kicked, it returns a card with a kicker ability — only kicker cards are legal targets") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Murasa Sproutling")
                .withLandsOnBattlefield(1, "Forest", 5)
                .withCardInGraveyard(1, "Ghitu Chronicler")
                .withCardInGraveyard(1, "Grizzly Bears")
                .withCardInGraveyard(1, "Steampath Charger")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val chronicler = game.findCardsInGraveyard(1, "Ghitu Chronicler").single()
            val bears = game.findCardsInGraveyard(1, "Grizzly Bears").single()
            val offspringCard = game.findCardsInGraveyard(1, "Steampath Charger").single()
            val sproutling = game.findCardsInHand(1, "Murasa Sproutling").single()

            game.execute(CastSpell(game.player1Id, sproutling, declaredCostSlot = ChoiceSlot.KICKED))
                .error shouldBe null
            game.resolveStack()

            val decision = game.getPendingDecision()
            decision.shouldBeInstanceOf<ChooseTargetsDecision>()
            val legal = decision.legalTargets[0].orEmpty()
            legal shouldContain chronicler
            legal shouldNotContain bears
            // Offspring rides the kicker rail but is not a kicker ability.
            legal shouldNotContain offspringCard

            game.selectTargets(listOf(chronicler)).error shouldBe null
            game.resolveStack()

            game.isInHand(1, "Ghitu Chronicler") shouldBe true
            game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            game.isInGraveyard(1, "Steampath Charger") shouldBe true
        }

        test("unkicked, it returns nothing") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Murasa Sproutling")
                .withLandsOnBattlefield(1, "Forest", 3)
                .withCardInGraveyard(1, "Ghitu Chronicler")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Murasa Sproutling").error shouldBe null
            game.resolveStack()

            game.hasPendingDecision() shouldBe false
            game.isInGraveyard(1, "Ghitu Chronicler") shouldBe true
        }
    }
}
