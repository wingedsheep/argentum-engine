package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.handlers.continuations.entityIdToChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.tmp.cards.AltarOfDementia
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Altar of Dementia.
 *
 * Altar of Dementia
 * {2}
 * Artifact
 * Sacrifice a creature: Target player mills cards equal to the sacrificed creature's power.
 */
class AltarOfDementiaScenarioTest : FunSpec({

    val abilityId = AltarOfDementia.activatedAbilities.first().id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(AltarOfDementia)
        return driver
    }

    fun librarySize(driver: GameTestDriver, playerId: com.wingedsheep.sdk.model.EntityId): Int =
        driver.state.getZone(com.wingedsheep.engine.state.ZoneKey(playerId, Zone.LIBRARY)).size

    test("opponent mills cards equal to the sacrificed creature's power") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val altar = driver.putPermanentOnBattlefield(activePlayer, "Altar of Dementia")
        // Grizzly Bears is a 2/2 — milling 2 cards.
        val bear = driver.putCreatureOnBattlefield(activePlayer, "Grizzly Bears")

        val oppLibBefore = librarySize(driver, opponent)
        val oppGraveBefore = driver.state.getGraveyard(opponent).size

        val result = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = altar,
                abilityId = abilityId,
                targets = listOf(entityIdToChosenTarget(driver.state, opponent)),
                costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(bear))
            )
        )
        result.isSuccess shouldBe true

        driver.bothPass()

        // Opponent milled 2 (Grizzly Bears power)
        librarySize(driver, opponent) shouldBe oppLibBefore - 2
        driver.state.getGraveyard(opponent).size shouldBe oppGraveBefore + 2
        // Sacrificed creature is gone
        driver.findPermanent(activePlayer, "Grizzly Bears") shouldBe null
    }

    test("can target self to mill own library") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val altar = driver.putPermanentOnBattlefield(activePlayer, "Altar of Dementia")
        val bear = driver.putCreatureOnBattlefield(activePlayer, "Grizzly Bears")

        val ownLibBefore = librarySize(driver, activePlayer)

        val result = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = altar,
                abilityId = abilityId,
                targets = listOf(entityIdToChosenTarget(driver.state, activePlayer)),
                costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(bear))
            )
        )
        result.isSuccess shouldBe true
        driver.bothPass()

        librarySize(driver, activePlayer) shouldBe ownLibBefore - 2
    }

    test("cannot activate without sacrificing a creature") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val altar = driver.putPermanentOnBattlefield(activePlayer, "Altar of Dementia")

        val result = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = altar,
                abilityId = abilityId,
                targets = listOf(entityIdToChosenTarget(driver.state, opponent)),
                costPayment = AdditionalCostPayment(sacrificedPermanents = emptyList())
            )
        )
        result.isSuccess shouldBe false
    }
})
