package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.drk.cards.Preacher
import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario tests for Preacher.
 *
 * The theft and the tap are one mechanism: control is bounded by WhileSourceTapped, so untapping the
 * Preacher hands the creature straight back. That is what a plain "gain control" would fail while
 * still passing a naive steal test, so both halves are checked.
 *
 * The target is the *opponent's* choice, so the controller activates with no targets at all and the
 * engine routes a ChooseTargetsDecision to them — the same shape Cuombajj Witches uses, and the
 * reason TargetObject needed the chooser field this card added.
 */
class PreacherScenarioTest : FunSpec({

    val abilityId = Preacher.activatedAbilities.first().id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(Preacher)
        return driver
    }

    test("steals while tapped and returns the creature when it untaps") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)

        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val preacher = driver.putCreatureOnBattlefield(me, "Preacher")
        driver.removeSummoningSickness(preacher)
        val prize = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")

        val activation = driver.submit(
            ActivateAbility(playerId = me, sourceId = preacher, abilityId = abilityId)
        )
        // A paused result is the success shape here: the ability is waiting on the opponent's
        // target choice, so `isSuccess` is false while `error` stays null.
        withClue("activation should not error: ${activation.error}") {
            activation.error shouldBe null
        }

        val decision = driver.pendingDecision
        decision.shouldBeInstanceOf<ChooseTargetsDecision>()
        withClue("the choice is routed to the opponent, not to me") {
            decision.playerId shouldBe opponent
        }
        driver.submitTargetSelection(opponent, listOf(prize)).error shouldBe null
        driver.bothPass()

        withClue("the Preacher tapped to pay, and the creature came across") {
            driver.isTapped(preacher) shouldBe true
            driver.state.projectedState.getController(prize) shouldBe me
        }

        withClue("untapping the Preacher ends the control effect") {
            driver.untapPermanent(preacher)
            driver.bothPass()
            driver.state.projectedState.getController(prize) shouldBe opponent
        }
    }

    test("the untap clause is optional, not mandatory") {
        withClue("MAY_NOT_UNTAP offers the choice; DOESNT_UNTAP would force it") {
            Preacher.flags.contains(AbilityFlag.MAY_NOT_UNTAP) shouldBe true
            Preacher.flags.contains(AbilityFlag.DOESNT_UNTAP) shouldBe false
        }
    }
})
