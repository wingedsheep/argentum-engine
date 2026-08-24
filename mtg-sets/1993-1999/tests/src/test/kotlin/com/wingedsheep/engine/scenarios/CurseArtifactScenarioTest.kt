package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Curse Artifact.
 *
 * Two things to pin. The tax falls on the *enchanted artifact's* controller, not on whoever cast
 * the Aura — so the Aura goes on an opponent's artifact and my own life must never move. And the
 * escape is "sacrifice **that** artifact": a second, uncursed artifact on the victim's board must
 * still be there afterwards, which is what an over-broad cost filter would get wrong.
 */
class CurseArtifactScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        return driver
    }

    fun attach(driver: GameTestDriver, auraId: EntityId, hostId: EntityId) {
        driver.addComponent(auraId, AttachedToComponent(hostId))
        val existing = driver.state.getEntity(hostId)?.get<AttachmentsComponent>()?.attachedIds ?: emptyList()
        driver.addComponent(hostId, AttachmentsComponent(existing + auraId))
    }

    /**
     * Drive the upkeep trigger to the end, paying the cost or declining it.
     *
     * A sacrifice cost asks with a [SelectCardsDecision] whose `minSelections` is 0 — declining is
     * "select nothing" — so neither `autoResolveDecision` (which takes `minSelections` options, i.e.
     * none, and therefore always declines) nor `submitYesNo` can express *paying*. The selection has
     * to be made explicitly.
     */
    fun settle(driver: GameTestDriver, payer: EntityId, pay: Boolean) {
        var guard = 0
        while (guard++ < 16 && (driver.state.stack.isNotEmpty() || driver.pendingDecision != null)) {
            when (val decision = driver.pendingDecision) {
                null -> driver.bothPass()
                is SelectCardsDecision -> driver.submitCardSelection(
                    payer,
                    if (pay) decision.options.take(decision.maxSelections) else emptyList()
                )
                else -> driver.submitYesNo(payer, pay)
            }
        }
    }

    test("declining costs the artifact's controller 2 life, and only their life") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)

        val me = driver.activePlayer!!
        val victim = driver.getOpponent(me)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val cursed = driver.putPermanentOnBattlefield(victim, "Fountain of Youth")
        // A second, uncursed artifact: an over-broad cost filter would have something else to eat.
        driver.putPermanentOnBattlefield(victim, "Fountain of Youth")
        val curse = driver.putPermanentOnBattlefield(me, "Curse Artifact")
        attach(driver, curse, cursed)

        withClue("my upkeep is not the one that fires") {
            driver.passPriorityUntil(Step.END)
            driver.getLifeTotal(victim) shouldBe 20
        }

        driver.passPriorityUntil(Step.UPKEEP)
        driver.activePlayer shouldBe victim
        settle(driver, victim, pay = false)

        withClue("the artifact's controller took 2, and I took none") {
            driver.getLifeTotal(victim) shouldBe 18
            driver.getLifeTotal(me) shouldBe 20
        }
        withClue("declining keeps both artifacts") {
            driver.getGraveyard(victim) shouldBe emptyList()
        }
    }

    test("the artifact's controller is actually offered the choice, and paying it saves the life") {
        // The declining case above passes whether or not a prompt ever appeared — taking 2 damage
        // is the outcome either way — which is how this shipped broken: the cost filter is
        // `attachedToBySource()`, and PayOrSuffer looked its candidates up with no source in the
        // predicate context, so the filter matched nothing, the cost read as unpayable, and the
        // damage was dealt with no question asked. Assert the decision exists, then take it.
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)

        val me = driver.activePlayer!!
        val victim = driver.getOpponent(me)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val cursed = driver.putPermanentOnBattlefield(victim, "Fountain of Youth")
        val spare = driver.putPermanentOnBattlefield(victim, "Fountain of Youth")
        val curse = driver.putPermanentOnBattlefield(me, "Curse Artifact")
        attach(driver, curse, cursed)

        driver.passPriorityUntil(Step.END)
        driver.passPriorityUntil(Step.UPKEEP)
        driver.activePlayer shouldBe victim

        var guard = 0
        while (guard++ < 16 && driver.pendingDecision == null && driver.state.stack.isNotEmpty()) {
            driver.bothPass()
        }

        withClue("the trigger asks rather than silently dealing the damage") {
            (driver.pendingDecision != null) shouldBe true
        }

        settle(driver, victim, pay = true)

        withClue("paying the cost sacrifices the cursed artifact and costs no life") {
            driver.getLifeTotal(victim) shouldBe 20
            driver.getGraveyard(victim) shouldBe listOf(cursed)
        }
        withClue("the uncursed artifact is not what got sacrificed") {
            (spare in driver.getGraveyard(victim)) shouldBe false
        }
    }
})
