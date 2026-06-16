package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.mana.CostCalculator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.fin.cards.TravelTheOverworld
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Travel the Overworld — {5}{U}{U} Sorcery
 *
 * "Affinity for Towns
 *  Draw four cards."
 */
class TravelTheOverworldTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(TravelTheOverworld)
        return driver
    }

    test("draws four cards") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.activePlayer!!

        val spell = driver.putCardInHand(me, "Travel the Overworld")
        val handBefore = driver.state.getHand(me).size

        driver.giveColorlessMana(me, 5)
        driver.giveMana(me, Color.BLUE, 2)
        driver.castSpell(me, spell)
        driver.bothPass()

        // Cast removed the spell (-1), draw 4 (+4) => net +3 over the pre-cast hand.
        driver.state.getHand(me).size shouldBe (handBefore - 1 + 4)
    }

    test("affinity for Towns reduces the generic cost per Town controlled") {
        val registry = CardRegistry()
        registry.register(TestCards.all)
        registry.register(TravelTheOverworld)
        val calculator = CostCalculator(registry)

        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.activePlayer!!

        // No Towns -> full {5} generic.
        val costBefore = calculator.calculateEffectiveCost(
            driver.state, registry.requireCard("Travel the Overworld"), me
        )
        costBefore.genericAmount shouldBe 5

        // Control two Town lands -> {5} - 2 = {3} generic.
        driver.putPermanentOnBattlefield(me, "Starting Town")
        driver.putPermanentOnBattlefield(me, "Starting Town")
        val costAfter = calculator.calculateEffectiveCost(
            driver.state, registry.requireCard("Travel the Overworld"), me
        )
        costAfter.genericAmount shouldBe 3
    }
})
