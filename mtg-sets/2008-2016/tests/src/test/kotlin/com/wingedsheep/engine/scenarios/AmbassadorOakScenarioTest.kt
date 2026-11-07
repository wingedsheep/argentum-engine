package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mor.cards.AmbassadorOak
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Ambassador Oak (Morningtide #113) — {3}{G} Creature — Treefolk Warrior 3/3.
 *
 * "When this creature enters, create a 1/1 green Elf Warrior creature token."
 */
class AmbassadorOakScenarioTest : FunSpec({

    test("entering creates a 1/1 green Elf Warrior token") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + AmbassadorOak)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.activePlayer!!

        val oak = driver.putCardInHand(me, "Ambassador Oak")
        driver.giveMana(me, Color.GREEN, 4)
        driver.castSpell(me, oak).outcome shouldBe Outcome.Done
        var guard = 0
        while (driver.state.stack.isNotEmpty() && guard++ < 10) driver.bothPass()

        val tokens = driver.getCreatures(me).filter { id ->
            val subtypes = driver.state.getEntity(id)?.get<CardComponent>()?.typeLine?.subtypes?.map { it.value }
            subtypes != null && "Elf" in subtypes && "Warrior" in subtypes
        }
        tokens.size shouldBe 1
        val token = tokens.single()
        val projected = driver.state.projectedState
        projected.getPower(token) shouldBe 1
        projected.getToughness(token) shouldBe 1
        projected.getColors(token) shouldBe setOf(Color.GREEN.name)
        driver.findPermanent(me, "Ambassador Oak") shouldBe oak
    }
})
