package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.handlers.continuations.entityIdToChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.drk.cards.BarlsCage
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Barl's Cage — "{3}: Target creature doesn't untap during its controller's
 * next untap step."
 *
 * The thing worth proving is the *duration's* anchor. `UntilAfterAffectedControllersNextUntap` is
 * keyed to the affected creature's controller, not the Cage's, so caging an opponent's creature has
 * to survive the caster's own untap step and be spent on the opponent's. A duration wired to the
 * source's controller would look identical on a mirror board and be wrong on this one.
 */
class BarlsCageScenarioTest : FunSpec({

    val abilityId = BarlsCage.activatedAbilities.first().id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(BarlsCage)
        return driver
    }

    test("a caged opponent's creature stays tapped through its controller's untap step") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)

        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val cage = driver.putPermanentOnBattlefield(me, "Barl's Cage")
        val bear = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        driver.tapPermanent(bear)
        driver.giveColorlessMana(me, 3)

        driver.submit(
            ActivateAbility(
                playerId = me,
                sourceId = cage,
                abilityId = abilityId,
                targets = listOf(entityIdToChosenTarget(driver.state, bear)),
            )
        ).isSuccess shouldBe true
        driver.bothPass()

        driver.passPriorityUntil(Step.END)
        withClue("my own untap step already passed and must not have spent the effect") {
            driver.isTapped(bear) shouldBe true
        }

        // Into the opponent's turn: their untap step is the one the effect is waiting for.
        driver.passPriorityUntil(Step.UPKEEP)
        driver.activePlayer shouldBe opponent
        withClue("the opponent's untap step is skipped for the caged creature") {
            driver.isTapped(bear) shouldBe true
        }
    }

    test("an uncaged creature untaps normally on the same turn cycle") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)

        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val bear = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        driver.tapPermanent(bear)

        driver.passPriorityUntil(Step.END)
        driver.passPriorityUntil(Step.UPKEEP)

        withClue("control: without the Cage the same creature untaps") {
            driver.activePlayer shouldBe opponent
            driver.isTapped(bear) shouldBe false
        }
    }
})
