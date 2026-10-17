package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseReplacementDecision
import com.wingedsheep.engine.core.DeclareBlockers
import com.wingedsheep.engine.core.ReplacementChosenResponse
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.drk.cards.MarshGoblins
import com.wingedsheep.mtg.sets.definitions.lea.cards.MagicalHack
import com.wingedsheep.mtg.sets.definitions.leg.cards.Quagmire
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContainIgnoringCase

/**
 * Quagmire (LEG) — {2}{B} Enchantment: "Creatures with swampwalk can be blocked as though they
 * didn't have swampwalk."
 */
class QuagmireScenarioTest : ScenarioTestBase() {

    private fun swampwalkerAttacksInto(quagmireController: Int?): TestGame {
        val builder = scenario()
            .withPlayers("Player1", "Player2")
            .withCardOnBattlefield(1, "Marsh Goblins")
            .withCardOnBattlefield(2, "Grizzly Bears")
            .withLandsOnBattlefield(2, "Swamp", 1)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        if (quagmireController != null) builder.withCardOnBattlefield(quagmireController, "Quagmire")
        val game = builder.build()
        game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        game.declareAttackers(mapOf("Marsh Goblins" to 2)).error shouldBe null
        game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
        return game
    }

    init {
        context("Quagmire") {
            test("a swampwalker can be blocked by a player who controls a Swamp") {
                val game = swampwalkerAttacksInto(quagmireController = 2)
                game.declareBlockers(mapOf("Grizzly Bears" to listOf("Marsh Goblins"))).error shouldBe null
            }

            test("it also works when the swampwalker's controller has the Quagmire") {
                val game = swampwalkerAttacksInto(quagmireController = 1)
                game.declareBlockers(mapOf("Grizzly Bears" to listOf("Marsh Goblins"))).error shouldBe null
            }

            test("without Quagmire, swampwalk keeps the attacker unblockable") {
                val game = swampwalkerAttacksInto(quagmireController = null)
                game.declareBlockers(mapOf("Grizzly Bears" to listOf("Marsh Goblins"))).error shouldNotBe null
            }

            test("Magical Hack turning its swamp into island makes it suppress islandwalk instead") {
                val driver = GameTestDriver()
                driver.registerCards(TestCards.all + listOf(MagicalHack, Quagmire, MarshGoblins))
                driver.initMirrorMatch(Deck.of("Island" to 30, "Swamp" to 30), skipMulligans = true)
                driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
                while (driver.activePlayer != driver.player1) { driver.bothPass(); driver.passPriorityUntil(Step.PRECOMBAT_MAIN) }
                val p1 = driver.player1
                val p2 = driver.player2
                val islandWalker = driver.putCreatureOnBattlefield(p1, "Island Walker")
                val swampWalker = driver.putCreatureOnBattlefield(p1, "Marsh Goblins")
                listOf(islandWalker, swampWalker).forEach(driver::removeSummoningSickness)
                val bears = driver.putCreatureOnBattlefield(p2, "Grizzly Bears")
                val bears2 = driver.putCreatureOnBattlefield(p2, "Grizzly Bears")
                driver.putLandOnBattlefield(p2, "Island")
                driver.putLandOnBattlefield(p2, "Swamp")
                val quagmire = driver.putPermanentOnBattlefield(p2, "Quagmire")

                val hack = driver.putCardInHand(p1, "Magical Hack")
                driver.giveMana(p1, Color.BLUE, 1)
                driver.castSpell(p1, hack, listOf(quagmire)).error shouldBe null
                driver.bothPass().error shouldBe null
                val decision = driver.pendingDecision as ChooseReplacementDecision
                driver.submitDecision(
                    p1, ReplacementChosenResponse(decision.id, decision.fromOptions.indexOf("Swamp"), decision.toOptions.indexOf("Island"))
                ).error shouldBe null

                driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
                driver.declareAttackers(p1, listOf(islandWalker, swampWalker), p2).error shouldBe null
                driver.bothPass()
                driver.submit(DeclareBlockers(p2, mapOf(bears2 to listOf(swampWalker)))).error shouldContainIgnoringCase "swampwalk"
                driver.submit(DeclareBlockers(p2, mapOf(bears to listOf(islandWalker)))).error shouldBe null
            }
        }
    }
}
