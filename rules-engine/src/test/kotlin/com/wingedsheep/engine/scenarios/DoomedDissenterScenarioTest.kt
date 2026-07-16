package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.akh.cards.DoomedDissenter
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Doomed Dissenter (AKH #87, reprinted VOW #? — canonical is AKH).
 *
 * {1}{B} Creature — Human, 1/1.
 * "When this creature dies, create a 2/2 black Zombie creature token."
 */
class DoomedDissenterScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(DoomedDissenter)
        return driver
    }

    test("dying creates a 2/2 black Zombie token") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 20, "Mountain" to 20), startingLife = 20)
        val me = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val dissenter = driver.putCreatureOnBattlefield(me, "Doomed Dissenter")

        driver.giveMana(me, Color.RED, 1)
        val bolt = driver.putCardInHand(me, "Lightning Bolt")
        val castResult = driver.castSpellWithTargets(
            me,
            bolt,
            listOf(com.wingedsheep.engine.state.components.stack.ChosenTarget.Permanent(dissenter))
        )
        castResult.isSuccess shouldBe true

        // Resolve the bolt: Doomed Dissenter (1/1) takes 3 damage and dies.
        driver.bothPass()
        driver.findPermanent(me, "Doomed Dissenter") shouldBe null

        // Resolve the dies trigger: create the Zombie token.
        driver.bothPass()

        val battlefield = driver.getPermanents(me)
        val zombieToken = battlefield.firstOrNull {
            driver.state.getEntity(it)?.get<CardComponent>()?.name == "Zombie Token"
        }
        val zombie = requireNotNull(zombieToken) { "Zombie token was not created" }

        val projector = com.wingedsheep.engine.mechanics.layers.StateProjector()
        projector.getProjectedPower(driver.state, zombie) shouldBe 2
        projector.getProjectedToughness(driver.state, zombie) shouldBe 2
        driver.state.getEntity(zombie)?.get<CardComponent>()?.colors shouldBe setOf(Color.BLACK)
    }
})
