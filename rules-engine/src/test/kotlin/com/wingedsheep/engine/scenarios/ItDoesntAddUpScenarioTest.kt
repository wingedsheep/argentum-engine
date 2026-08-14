package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mkm.cards.ItDoesntAddUp
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * It Doesn't Add Up (MKM #89) — {3}{B}{B} Instant.
 *
 * "Return target creature card from your graveyard to the battlefield. Suspect it."
 *
 * The composition under test is that the suspect lands on the *reanimated permanent*, not on some
 * stale graveyard-card reference: the card is targeted while it is in the graveyard, moved to the
 * battlefield, and then the same target reference has to address the permanent that just arrived.
 * If entity identity were not preserved across the zone move, the reanimation would still look
 * correct while the suspect silently no-opped — which is exactly the failure mode a "did it come
 * back?" assertion alone would miss.
 */
class ItDoesntAddUpScenarioTest : FunSpec({

    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(ItDoesntAddUp)
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("the reanimated creature comes back suspected") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        val courser = driver.putCardInGraveyard(player, "Centaur Courser")
        val spell = driver.putCardInHand(player, "It Doesn't Add Up")
        driver.giveMana(player, Color.BLACK, 2)
        driver.giveColorlessMana(player, 3)

        driver.castSpellWithTargets(
            player,
            spell,
            listOf(ChosenTarget.Card(courser, player, Zone.GRAVEYARD))
        ).error shouldBe null
        driver.bothPass()

        withClue("the creature card is on the battlefield under its owner's control") {
            driver.findPermanent(player, "Centaur Courser") shouldBe courser
        }

        val projected = projector.project(driver.state)
        withClue("and the same permanent is suspected — menace and can't block (CR 701.60a)") {
            projected.isSuspected(courser) shouldBe true
            projected.hasKeyword(courser, Keyword.MENACE) shouldBe true
            projected.cantBlock(courser) shouldBe true
        }
    }
})
