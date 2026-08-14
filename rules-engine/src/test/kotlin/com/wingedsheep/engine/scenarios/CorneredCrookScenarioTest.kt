package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mkm.cards.CorneredCrook
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Cornered Crook (MKM) — {4}{R} 5/4 Lizard Warrior.
 *
 * "When this creature enters, you may sacrifice an artifact. When you do, this creature deals 3
 *  damage to any target."
 *
 * The damage is a CR 603.12 reflexive ability, so the flow under test is: enters trigger → yes/no →
 * pick the artifact → *then* pick the damage target. These cover the happy path, the decline, and
 * the infeasible case where no artifact exists and the question must not be asked at all.
 */
class CorneredCrookScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(CorneredCrook))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun castCrook(driver: GameTestDriver, player: EntityId) {
        val card = driver.putCardInHand(player, "Cornered Crook")
        driver.giveMana(player, Color.RED, 1)
        driver.giveColorlessMana(player, 4)
        driver.castSpell(player, card).error shouldBe null
        driver.bothPass() // resolve the creature spell
        driver.bothPass() // enters trigger goes on the stack and resolves
    }

    test("sacrificing an artifact deals 3 damage to the chosen target") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)

        val artifact = driver.putCreatureOnBattlefield(me, "Artifact Creature")

        castCrook(driver, me)

        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        driver.submitYesNo(me, true)
        driver.submitTargetSelection(me, listOf(artifact))

        withClue("the artifact is gone before the reflexive ability picks a target") {
            driver.getGraveyard(me).contains(artifact) shouldBe true
        }

        // "When you do" — a second ability, targeted only now.
        driver.submitTargetSelection(me, listOf(opponent))
        driver.bothPass()

        driver.assertLifeTotal(opponent, 17)
    }

    test("declining the sacrifice deals no damage and keeps the artifact") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)

        val artifact = driver.putCreatureOnBattlefield(me, "Artifact Creature")

        castCrook(driver, me)

        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        driver.submitYesNo(me, false)

        withClue("no sacrifice means the reflexive ability never triggers") {
            driver.state.getBattlefield(me).contains(artifact) shouldBe true
            driver.assertLifeTotal(opponent, 20)
        }
    }

    test("with no artifact to sacrifice, the question is never asked") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)

        castCrook(driver, me)

        withClue("an impossible action must not prompt — a 'yes' could only no-op") {
            driver.pendingDecision shouldBe null
            driver.assertLifeTotal(opponent, 20)
        }
    }

    test("the damage can be aimed at a creature instead of a player") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)

        val artifact = driver.putCreatureOnBattlefield(me, "Artifact Creature")
        val victim = driver.putCreatureOnBattlefield(opponent, "Centaur Courser") // 3/3

        castCrook(driver, me)

        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        driver.submitYesNo(me, true)
        driver.submitTargetSelection(me, listOf(artifact))
        driver.submitTargetSelection(me, listOf(victim))
        driver.bothPass()

        withClue("3 damage kills a 3/3") {
            driver.state.getBattlefield(opponent).contains(victim) shouldBe false
            driver.assertLifeTotal(opponent, 20)
        }
    }
})
