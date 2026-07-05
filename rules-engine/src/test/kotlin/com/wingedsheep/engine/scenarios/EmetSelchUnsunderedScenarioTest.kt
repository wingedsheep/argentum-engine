package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Emet-Selch, Unsundered // Hades, Sorcerer of Eld (FIN #218).
 *
 * Covers the assembled behaviour:
 *  - The upkeep intervening-"if" transform: with fourteen or more cards in your graveyard the
 *    "you may transform" ability triggers and, on yes, flips to Hades; with only thirteen it never
 *    triggers (so no decision is offered and it stays Emet-Selch).
 *  - Hades' back-face replacement: a card you own that would be put into your graveyard from
 *    anywhere is exiled instead.
 */
class EmetSelchUnsunderedScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    /** Pass whole turns until it is [player]'s own upkeep. */
    fun advanceToUpkeepOf(driver: GameTestDriver, player: EntityId) {
        var guard = 0
        while (guard++ < 6) {
            driver.passPriorityUntil(Step.UPKEEP, maxPasses = 200)
            if (driver.activePlayer == player) break
            // Already at an upkeep: step out of it so the next call advances to the following one.
            driver.passPriorityUntil(Step.PRECOMBAT_MAIN, maxPasses = 200)
        }
        driver.activePlayer shouldBe player
    }

    /** Resolve the stack until a yes/no ("may") decision is pending, or the stack empties. */
    fun resolveUntilMayDecision(driver: GameTestDriver) {
        var guard = 0
        while (driver.pendingDecision !is YesNoDecision && guard++ < 40) {
            if (driver.state.stack.isNotEmpty() || driver.isPaused) driver.bothPass() else break
        }
    }

    test("upkeep: with fourteen cards in your graveyard you may transform into Hades") {
        val driver = createDriver()
        val you = driver.player1

        val emet = driver.putCreatureOnBattlefield(you, "Emet-Selch, Unsundered")
        repeat(14) { driver.putCardInGraveyard(you, "Swamp") }

        advanceToUpkeepOf(driver, you)
        resolveUntilMayDecision(driver)

        // The intervening-"if" is satisfied, so the "you may transform" ability is offered.
        val may = driver.pendingDecision
        (may is YesNoDecision) shouldBe true
        driver.submitYesNo(you, true)

        driver.state.getEntity(emet)!!.get<CardComponent>()!!.name shouldBe "Hades, Sorcerer of Eld"
    }

    test("upkeep: with only thirteen cards the transform never triggers") {
        val driver = createDriver()
        val you = driver.player1

        val emet = driver.putCreatureOnBattlefield(you, "Emet-Selch, Unsundered")
        repeat(13) { driver.putCardInGraveyard(you, "Swamp") }

        advanceToUpkeepOf(driver, you)
        resolveUntilMayDecision(driver)

        // Intervening-"if" fails at trigger time → no ability, no decision, still the front face.
        (driver.pendingDecision is YesNoDecision) shouldBe false
        driver.state.getEntity(emet)!!.get<CardComponent>()!!.name shouldBe "Emet-Selch, Unsundered"
    }

    test("back face: a creature you own that would die is exiled instead of hitting the graveyard") {
        val driver = createDriver()
        val you = driver.player1

        // Flip Emet-Selch to Hades via the upkeep transform.
        val emet = driver.putCreatureOnBattlefield(you, "Emet-Selch, Unsundered")
        repeat(14) { driver.putCardInGraveyard(you, "Swamp") }
        advanceToUpkeepOf(driver, you)
        resolveUntilMayDecision(driver)
        driver.submitYesNo(you, true)
        driver.state.getEntity(emet)!!.get<CardComponent>()!!.name shouldBe "Hades, Sorcerer of Eld"

        // A 1/1 you control, killed by your own Lightning Bolt, should be exiled — not put into
        // your graveyard — by Hades' replacement effect.
        val lion = driver.putCreatureOnBattlefield(you, "Savannah Lions")
        val bolt = driver.putCardInHand(you, "Lightning Bolt")
        driver.giveMana(you, Color.RED, 1)
        driver.castSpell(you, bolt, listOf(lion)).isSuccess shouldBe true
        var g = 0
        while (driver.state.stack.isNotEmpty() && g++ < 20) driver.bothPass()

        driver.getExile(you).contains(lion) shouldBe true
        driver.getGraveyard(you).contains(lion) shouldBe false
    }
})
