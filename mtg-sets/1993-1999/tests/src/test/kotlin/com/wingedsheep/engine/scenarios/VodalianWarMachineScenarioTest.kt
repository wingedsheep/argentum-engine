package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.addFloatingEffect
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.fem.cards.VodalianWarMachine
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Vodalian War Machine (Fallen Empires).
 *
 * The clause worth pinning is the death trigger: "destroy all Merfolk tapped this turn to pay for
 * its abilities" has to remember *which* Merfolk paid, across activations of either ability, and
 * must leave alone a Merfolk that was tapped for something else.
 */
class VodalianWarMachineScenarioTest : FunSpec({

    val attackAbility = VodalianWarMachine.activatedAbilities[0].id
    val pumpAbility = VodalianWarMachine.activatedAbilities[1].id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(VodalianWarMachine)
        return driver
    }

    test("the Merfolk that paid are destroyed when the War Machine dies; a bystander is not") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)

        val alice = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val machine = driver.putCreatureOnBattlefield(alice, "Vodalian War Machine")
        driver.removeSummoningSickness(machine)
        val payer1 = driver.putCreatureOnBattlefield(alice, "Vodalian Soldiers")
        val payer2 = driver.putCreatureOnBattlefield(alice, "Vodalian Soldiers")
        val bystander = driver.putCreatureOnBattlefield(alice, "Vodalian Soldiers")
        listOf(payer1, payer2, bystander).forEach { driver.removeSummoningSickness(it) }

        driver.submitSuccess(
            ActivateAbility(
                playerId = alice, sourceId = machine, abilityId = attackAbility,
                costPayment = com.wingedsheep.sdk.scripting.AdditionalCostPayment(
                    tappedPermanents = listOf(payer1)
                )
            )
        )
        driver.submitSuccess(
            ActivateAbility(
                playerId = alice, sourceId = machine, abilityId = pumpAbility,
                costPayment = com.wingedsheep.sdk.scripting.AdditionalCostPayment(
                    tappedPermanents = listOf(payer2)
                )
            )
        )
        driver.bothPass()

        // Kill it for real — `moveToGraveyard` is a blunt zone move that deliberately skips dies
        // triggers, and the dies trigger is the whole point here. -0/-8 covers the 0/4 Wall plus
        // the +2/+1 it just gave itself.
        driver.replaceState(
            driver.state.addFloatingEffect(
                layer = Layer.POWER_TOUGHNESS,
                modification = SerializableModification.ModifyPowerToughness(powerMod = 0, toughnessMod = -8),
                affectedEntities = setOf(machine),
                duration = Duration.EndOfTurn,
                context = EffectContext(sourceId = machine, controllerId = alice),
            )
        )
        // Let the engine notice it: passing priority runs state-based actions *and* the trigger
        // detection that follows them, which a direct StateBasedActionChecker call would skip.
        driver.passPriorityUntil(Step.END)

        withClue("the War Machine actually died") {
            driver.state.getBattlefield().contains(machine) shouldBe false
        }
        withClue("both Merfolk that paid were destroyed") {
            driver.state.getBattlefield().contains(payer1) shouldBe false
            driver.state.getBattlefield().contains(payer2) shouldBe false
        }
        withClue("the Merfolk that paid for nothing survives") {
            driver.state.getBattlefield().contains(bystander) shouldBe true
        }
    }
})
