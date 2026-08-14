package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mkm.cards.AgencyCoroner
import com.wingedsheep.mtg.sets.definitions.mkm.cards.RepeatOffender
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Agency Coroner — "{2}{B}, Sacrifice another creature: Draw a card. If the sacrificed creature was
 * suspected, draw two cards instead."
 *
 * Covers the new `Conditions.SacrificedWasSuspected` vocabulary and the `EntitySnapshot.wasSuspected`
 * flag behind it. The point of the test is the *timing*: the suspected designation is a floating
 * effect keyed on the creature, so it is destroyed along with the creature the moment the cost is
 * paid — well before the ability resolves. If the condition read live state instead of the frozen
 * cost-time snapshot (CR 608.2h), the suspected case would silently draw one card, which is exactly
 * the failure this test pins down.
 *
 * Repeat Offender supplies the suspect: its own `{2}{B}` ability suspects it when it isn't already.
 */
class AgencyCoronerScenarioTest : FunSpec({

    val coronerAbility = AgencyCoroner.activatedAbilities.first().id
    val offenderAbility = RepeatOffender.activatedAbilities.first().id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(AgencyCoroner)
        driver.registerCard(RepeatOffender)
        return driver
    }

    test("sacrificing a suspected creature draws two cards") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true)
        val active = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val coroner = driver.putCreatureOnBattlefield(active, "Agency Coroner")
        val offender = driver.putCreatureOnBattlefield(active, "Repeat Offender")

        // Suspect the Offender with its own ability.
        driver.giveMana(active, Color.BLACK, 3)
        driver.submitSuccess(ActivateAbility(active, offender, offenderAbility))
        driver.bothPass()
        withClue("Repeat Offender's ability suspects it when it isn't already suspected") {
            StateProjector().project(driver.state).isSuspected(offender) shouldBe true
        }

        val handBefore = driver.getHandSize(active)

        driver.giveMana(active, Color.BLACK, 3)
        driver.submitSuccess(
            ActivateAbility(
                playerId = active,
                sourceId = coroner,
                abilityId = coronerAbility,
                costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(offender))
            )
        )
        driver.bothPass()

        withClue("the suspected body is worth two cards, read from the cost-time snapshot") {
            driver.getHandSize(active) shouldBe handBefore + 2
        }
        driver.assertInGraveyard(active, "Repeat Offender")
    }

    test("sacrificing an unsuspected creature draws one card") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true)
        val active = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val coroner = driver.putCreatureOnBattlefield(active, "Agency Coroner")
        val bear = driver.putCreatureOnBattlefield(active, "Grizzly Bears")
        withClue("a plain creature is not suspected") {
            StateProjector().project(driver.state).isSuspected(bear) shouldBe false
        }

        val handBefore = driver.getHandSize(active)

        driver.giveMana(active, Color.BLACK, 3)
        driver.submitSuccess(
            ActivateAbility(
                playerId = active,
                sourceId = coroner,
                abilityId = coronerAbility,
                costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(bear))
            )
        )
        driver.bothPass()

        withClue("no suspect, no bonus — the else branch draws exactly one") {
            driver.getHandSize(active) shouldBe handBefore + 1
        }
    }
})
