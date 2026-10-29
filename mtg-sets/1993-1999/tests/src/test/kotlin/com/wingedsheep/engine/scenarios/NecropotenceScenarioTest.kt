package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ice.cards.Necropotence
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe

/**
 * Necropotence (ICE #154) — "Pay 1 life: Exile the top card of your library face down. Put that
 * card into your hand at the beginning of your next end step."
 *
 * Pins the delayed return: the exiled card waits for *your* next end step, and an activation
 * during your own end step defers it to your following turn's end step rather than the
 * opponent's.
 */
class NecropotenceScenarioTest : FunSpec({
    val payLifeAbilityId = Necropotence.activatedAbilities[0].id

    fun newDriver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(Necropotence))
        initMirrorMatch(Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.settle(maxSteps: Int = 40) {
        repeat(maxSteps) {
            when {
                pendingDecision != null -> autoResolveDecision()
                state.stack.isNotEmpty() -> bothPass()
                else -> return
            }
        }
    }

    fun GameTestDriver.payLife(me: EntityId, necro: EntityId): EntityId {
        val top = putCardOnTopOfLibrary(me, "Lightning Bolt")
        submitSuccess(ActivateAbility(playerId = me, sourceId = necro, abilityId = payLifeAbilityId))
        settle()
        return top
    }

    test("the exiled card comes to hand at the beginning of your end step") {
        val driver = newDriver()
        val me = driver.player1
        val necro = driver.putPermanentOnBattlefield(me, "Necropotence")

        val exiled = driver.payLife(me, necro)

        driver.getLifeTotal(me) shouldBe 19
        driver.state.getZone(me, Zone.EXILE).contains(exiled).shouldBeTrue()
        driver.state.getZone(me, Zone.HAND).contains(exiled).shouldBeFalse()

        driver.passPriorityUntil(Step.END)
        driver.settle()

        withClue("the delayed trigger returns the card at your end step") {
            driver.state.getZone(me, Zone.HAND).contains(exiled).shouldBeTrue()
            driver.state.getZone(me, Zone.EXILE).contains(exiled).shouldBeFalse()
        }
    }

    test("activating during your end step waits for your next turn's end step") {
        val driver = newDriver()
        val me = driver.player1
        val opponent = driver.getOpponent(me)
        val necro = driver.putPermanentOnBattlefield(me, "Necropotence")

        driver.passPriorityUntil(Step.END)
        driver.settle()
        val exiled = driver.payLife(me, necro)

        driver.passPriorityUntil(Step.END, opponent)
        driver.settle()
        withClue("the opponent's end step is not your next end step") {
            driver.state.getZone(me, Zone.EXILE).contains(exiled).shouldBeTrue()
        }

        driver.passPriorityUntil(Step.END, me)
        driver.settle()
        withClue("it returns at your following end step") {
            driver.state.getZone(me, Zone.HAND).contains(exiled).shouldBeTrue()
        }
    }
})
