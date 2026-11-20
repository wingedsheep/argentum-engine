package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Chained to the Rocks (THS #4) — {W} Enchantment — Aura.
 *
 *   Enchant Mountain you control
 *   When this Aura enters, exile target creature an opponent controls until this Aura leaves the
 *   battlefield. (That creature returns under its owner's control.)
 */
class ChainedToTheRocksScenarioTest : ScenarioTestBase() {

    private fun board(): TestGame =
        scenario()
            .withPlayers("Player1", "Player2")
            .withCardInHand(1, "Chained to the Rocks")
            .withCardInHand(1, "Naturalize")
            .withLandsOnBattlefield(1, "Mountain", 1)
            .withLandsOnBattlefield(1, "Plains", 1)
            .withLandsOnBattlefield(1, "Forest", 2)
            .withCardOnBattlefield(2, "Grizzly Bears")
            .withCardInLibrary(1, "Plains")
            .withCardInLibrary(2, "Plains")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

    init {
        test("exiles an opponent's creature until the Aura leaves, then it returns") {
            val game = board()
            val mountain = game.findPermanent("Mountain")!!
            val bears = game.findPermanent("Grizzly Bears")!!

            game.castSpell(1, "Chained to the Rocks", mountain).error shouldBe null
            game.resolveStack()
            game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
            game.selectTargets(listOf(bears)).error shouldBe null
            game.resolveStack()

            game.findPermanent("Grizzly Bears") shouldBe null
            game.isInExile(2, "Grizzly Bears") shouldBe true

            val chained = game.findPermanent("Chained to the Rocks")!!
            game.castSpell(1, "Naturalize", chained).error shouldBe null
            game.resolveStack()

            game.findPermanent("Chained to the Rocks") shouldBe null
            game.isInExile(2, "Grizzly Bears") shouldBe false
            game.findPermanent("Grizzly Bears") shouldNotBe null
        }

        test("it can only enchant a Mountain you control") {
            val game = board()
            val plains = game.findPermanent("Plains")!!

            game.castSpell(1, "Chained to the Rocks", plains).error shouldNotBe null
            game.isInHand(1, "Chained to the Rocks") shouldBe true
        }
    }
}
