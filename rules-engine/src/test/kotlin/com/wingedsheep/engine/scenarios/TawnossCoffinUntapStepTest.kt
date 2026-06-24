package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.atq.cards.TawnossCoffin
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Regression test for Tawnos's Coffin's "When this artifact … becomes untapped, return that exiled
 * card …" trigger firing during the controller's own UNTAP STEP.
 *
 * The Coffin carries [com.wingedsheep.sdk.core.AbilityFlag.MAY_NOT_UNTAP], so untapping it in the
 * untap step routes through the "choose which permanents to keep tapped" decision. That decision
 * resumes via `SubmitDecisionHandler`, which used to run trigger detection only on the step-advance
 * events and dropped the `UntappedEvent`s emitted while resolving the choice — so the Coffin
 * untapped but its becomes-untapped trigger never fired and the exiled creature stayed exiled.
 *
 * The existing [TawnossCoffinScenarioTest] only exercises an `Untap` *spell* (which fires the
 * trigger through the normal priority-point detection), so this gap was invisible there.
 */
class TawnossCoffinUntapStepTest : FunSpec({

    val coffinAbilityId = TawnossCoffin.activatedAbilities.first().id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        return driver
    }

    test("becomes-untapped trigger returns the exiled creature when the Coffin untaps in the untap step") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val coffin = driver.putPermanentOnBattlefield(activePlayer, "Tawnos's Coffin")
        val bear = driver.putCreatureOnBattlefield(activePlayer, "Grizzly Bears")

        // {3}, {T}: exile the bear and note its (zero) counters.
        driver.giveMana(activePlayer, Color.GREEN, 3)
        driver.submitSuccess(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = coffin,
                abilityId = coffinAbilityId,
                targets = listOf(ChosenTarget.Permanent(bear))
            )
        )
        driver.bothPass()

        withClue("Grizzly Bears is exiled by the Coffin's ability") {
            driver.findPermanent(activePlayer, "Grizzly Bears") shouldBe null
        }
        withClue("the Coffin is tapped after paying its {T} cost") {
            driver.state.getEntity(coffin)!!.has<TappedComponent>() shouldBe true
        }

        // Advance to the controller's own untap step. The engine only rests at UNTAP because the
        // Coffin's MAY_NOT_UNTAP decision pauses there; otherwise it blows straight through.
        driver.passPriorityUntil(Step.UNTAP)
        withClue("the MAY_NOT_UNTAP keep-tapped decision is pending") {
            (driver.pendingDecision is SelectCardsDecision) shouldBe true
        }

        // Choose NOT to keep the Coffin tapped → it untaps, firing the becomes-untapped trigger.
        driver.submitCardSelection(activePlayer, emptyList())
        withClue("the Coffin untapped during the untap step") {
            driver.state.getEntity(coffin)!!.has<TappedComponent>() shouldBe false
        }

        // The trigger was put on the stack at the upkeep priority point; resolve it.
        driver.bothPass()

        val returnedBear = driver.findPermanent(activePlayer, "Grizzly Bears")
        withClue("the exiled creature returns to the battlefield when the Coffin untaps") {
            returnedBear.shouldNotBeNull()
        }
        withClue("it returns tapped, per the Coffin's reminder text") {
            driver.state.getEntity(returnedBear!!)!!.has<TappedComponent>() shouldBe true
        }
    }
})
