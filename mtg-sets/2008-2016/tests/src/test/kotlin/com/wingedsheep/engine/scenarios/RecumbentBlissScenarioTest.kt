package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.eve.cards.RecumbentBliss
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Recumbent Bliss (Eventide #13) — {2}{W} Enchantment — Aura.
 * "Enchant creature / Enchanted creature can't attack or block. / At the beginning of your upkeep, you may
 *  gain 1 life."
 */
class RecumbentBlissScenarioTest : ScenarioTestBase() {
    init {
        test("the enchanted creature can't attack") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardAttachedTo(2, "Recumbent Bliss", "Grizzly Bears")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldNotBe null
        }

        test("the enchanted creature can't block") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Child of Night")
                .withCardAttachedTo(2, "Recumbent Bliss", "Grizzly Bears")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Child of Night" to 1)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(mapOf("Grizzly Bears" to listOf("Child of Night"))).error shouldNotBe null
        }

        test("its controller may gain 1 life at the beginning of their upkeep") {
            val driver = GameTestDriver()
            driver.registerCards(TestCards.all + RecumbentBliss)
            driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
            driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
            val me = driver.activePlayer!!
            val opponent = driver.getOpponent(me)
            val bears = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")

            val bliss = driver.putCardInHand(me, "Recumbent Bliss")
            driver.giveMana(me, Color.WHITE, 3)
            driver.castSpell(me, bliss, targets = listOf(bears)).outcome shouldBe Outcome.Done
            driver.bothPass()
            driver.findPermanent(me, "Recumbent Bliss") shouldNotBe null

            driver.passPriorityUntil(Step.UPKEEP, activePlayer = me)
            var guard = 0
            while (driver.state.pendingDecision !is YesNoDecision && guard++ < 10) driver.bothPass()
            driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
            driver.submitYesNo(me, true)
            guard = 0
            while (driver.state.stack.isNotEmpty() && guard++ < 10) driver.bothPass()

            driver.getLifeTotal(me) shouldBe 21
        }
    }
}
