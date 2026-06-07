package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.handlers.continuations.entityIdToChosenTarget
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.tmp.cards.JinxedIdol
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Jinxed Idol.
 *
 * Jinxed Idol
 * {2}
 * Artifact
 * At the beginning of your upkeep, this artifact deals 2 damage to you.
 * Sacrifice a creature: Target opponent gains control of this artifact.
 *
 * In a 2-player game, TargetOpponent auto-selects the single opponent.
 */
class JinxedIdolScenarioTest : FunSpec({

    val abilityId = JinxedIdol.activatedAbilities.first().id
    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(JinxedIdol)
        return driver
    }

    test("upkeep trigger deals 2 damage to controller") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.putPermanentOnBattlefield(activePlayer, "Jinxed Idol")

        val lifeBefore = driver.getLifeTotal(activePlayer)

        // Advance to the controller's next upkeep.
        driver.passPriorityUntil(Step.DRAW, maxPasses = 200)
        driver.passPriorityUntil(Step.DRAW, maxPasses = 200)
        driver.passPriorityUntil(Step.UPKEEP, maxPasses = 200)
        driver.currentStep shouldBe Step.UPKEEP
        driver.activePlayer shouldBe activePlayer

        // Resolve the upkeep trigger.
        driver.bothPass()

        driver.getLifeTotal(activePlayer) shouldBe lifeBefore - 2
    }

    test("sacrifice a creature to give the idol to target opponent") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val idol = driver.putPermanentOnBattlefield(activePlayer, "Jinxed Idol")
        val bear = driver.putCreatureOnBattlefield(activePlayer, "Grizzly Bears")

        projector.project(driver.state).getController(idol) shouldBe activePlayer

        val result = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = idol,
                abilityId = abilityId,
                targets = listOf(entityIdToChosenTarget(driver.state, opponent)),
                costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(bear))
            )
        )
        result.isSuccess shouldBe true
        driver.bothPass()

        // Opponent now controls the idol (control change is a Layer.CONTROL floating effect).
        projector.project(driver.state).getController(idol) shouldBe opponent
        // Sacrificed creature is gone.
        driver.findPermanent(activePlayer, "Grizzly Bears") shouldBe null
    }

    test("cannot activate without sacrificing a creature") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val idol = driver.putPermanentOnBattlefield(activePlayer, "Jinxed Idol")

        val result = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = idol,
                abilityId = abilityId,
                costPayment = AdditionalCostPayment(sacrificedPermanents = emptyList())
            )
        )
        result.isSuccess shouldBe false
    }
})
