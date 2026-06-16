package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.mtg.sets.definitions.otj.cards.SpinewoodsArmadillo
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Spinewoods Armadillo (OTJ #182) — {4}{G}{G} 7/7 Creature — Armadillo.
 *
 * "Reach
 *  Ward {3}
 *  {1}{G}, Discard this card: Search your library for a basic land card or a Desert card,
 *  reveal it, put it into your hand, then shuffle. You gain 3 life."
 *
 * Exercises the from-hand activated ability: paying {1}{G} and discarding this card fetches a
 * basic/Desert land into hand and gains 3 life. (Reach/Ward are the named keywords.)
 */
class SpinewoodsArmadilloScenarioTest : FunSpec({

    val abilityId = SpinewoodsArmadillo.activatedAbilities.first().id

    test("from-hand ability fetches a land into hand and gains 3 life, discarding this card") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Grizzly Bears" to 40), startingLife = 20)
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val forest = driver.putCardOnTopOfLibrary(player, "Forest")
        val armadillo = driver.putCardInHand(player, "Spinewoods Armadillo")
        driver.giveMana(player, Color.GREEN, 2)

        val lifeBefore = driver.state.getEntity(player)!!.get<LifeTotalComponent>()!!.life

        driver.submit(
            ActivateAbility(playerId = player, sourceId = armadillo, abilityId = abilityId)
        ).isSuccess shouldBe true
        driver.bothPass()

        // Search decision for the basic land.
        if (driver.isPaused) {
            val decision = driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
            decision.options.contains(forest) shouldBe true
            driver.submitCardSelection(player, listOf(forest))
        }
        driver.isPaused shouldBe false

        // Armadillo was discarded (no longer in hand), land is now in hand, +3 life.
        driver.getHand(player).contains(armadillo) shouldBe false
        driver.getGraveyard(player).contains(armadillo) shouldBe true
        driver.getHand(player).contains(forest) shouldBe true
        driver.state.getEntity(player)!!.get<LifeTotalComponent>()!!.life shouldBe lifeBefore + 3
    }
})
