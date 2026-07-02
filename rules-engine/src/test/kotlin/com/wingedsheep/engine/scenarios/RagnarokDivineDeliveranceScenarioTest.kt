package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.fin.cards.RagnarokDivineDeliverance
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Ragnarok, Divine Deliverance (FIN) — meld back of Vanille + Fang, authored as a plain legendary
 * (see the card comment). It's a 7/6 with vigilance, menace, trample, reach, haste and a dies
 * trigger: "destroy target permanent and return target nonlegendary permanent card from your
 * graveyard to the battlefield."
 *
 * Test 1 (keywords) confirms all five keywords project onto Ragnarok.
 *
 * Test 2 (dies trigger) drives the payoff: Ragnarok is killed by two Lightning Bolts (6 damage on
 * the 6-toughness body). Its dies trigger picks two independent targets — an opponent's permanent
 * to destroy and a nonlegendary creature card in the controller's graveyard to reanimate — and on
 * resolution the opponent's permanent is destroyed and the graveyard card returns to the
 * battlefield under the controller.
 */
class RagnarokDivineDeliveranceScenarioTest : FunSpec({

    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(RagnarokDivineDeliverance))
        return driver
    }

    test("Ragnarok has vigilance, menace, trample, reach, and haste") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)

        val ragnarok = driver.putCreatureOnBattlefield(driver.player1, "Ragnarok, Divine Deliverance")

        val projected = projector.project(driver.state)
        projected.hasKeyword(ragnarok, Keyword.VIGILANCE) shouldBe true
        projected.hasKeyword(ragnarok, Keyword.MENACE) shouldBe true
        projected.hasKeyword(ragnarok, Keyword.TRAMPLE) shouldBe true
        projected.hasKeyword(ragnarok, Keyword.REACH) shouldBe true
        projected.hasKeyword(ragnarok, Keyword.HASTE) shouldBe true
    }

    test("dies: destroy target permanent and reanimate a nonlegendary permanent card from your graveyard") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.activePlayer shouldBe driver.player1

        val ragnarok = driver.putCreatureOnBattlefield(driver.player1, "Ragnarok, Divine Deliverance")
        // A nonlegendary permanent card in the controller's graveyard is the reanimation target.
        val reanimateTarget = driver.putCardInGraveyard(driver.player1, "Grizzly Bears")
        // A permanent the opponent controls is the destroy target.
        val destroyTarget = driver.putCreatureOnBattlefield(driver.player2, "Grizzly Bears")

        // Kill Ragnarok with two Lightning Bolts (3 + 3 = 6 damage on the 6-toughness body).
        val bolt1 = driver.putCardInHand(driver.player1, "Lightning Bolt")
        val bolt2 = driver.putCardInHand(driver.player1, "Lightning Bolt")
        driver.giveMana(driver.player1, Color.RED, 2)

        driver.castSpell(driver.player1, bolt1, targets = listOf(ragnarok)).isSuccess shouldBe true
        driver.bothPass() // resolve first bolt — 3 damage, Ragnarok survives
        driver.state.getBattlefield().contains(ragnarok) shouldBe true

        driver.castSpell(driver.player1, bolt2, targets = listOf(ragnarok)).isSuccess shouldBe true
        driver.bothPass() // resolve second bolt — 6 damage, Ragnarok dies, queuing its trigger

        driver.state.getBattlefield().contains(ragnarok) shouldBe false

        // The dies trigger asks for both targets in one decision (index 0 = destroy, 1 = reanimate).
        driver.pendingDecision as ChooseTargetsDecision
        driver.submitMultiTargetSelection(
            driver.player1,
            mapOf(0 to listOf(destroyTarget), 1 to listOf(reanimateTarget)),
        )
        driver.bothPass() // resolve the dies trigger

        // Opponent's permanent is destroyed.
        driver.state.getBattlefield().contains(destroyTarget) shouldBe false
        // The graveyard card is reanimated under the controller (getBattlefield(player) filters by controller).
        driver.getGraveyard(driver.player1).contains(reanimateTarget) shouldBe false
        driver.state.getBattlefield(driver.player1).contains(reanimateTarget) shouldBe true
    }
})
