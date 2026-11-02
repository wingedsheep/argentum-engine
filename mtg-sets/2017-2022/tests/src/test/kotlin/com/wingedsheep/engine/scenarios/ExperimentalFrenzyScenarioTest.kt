package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.grn.cards.ExperimentalFrenzy
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Experimental Frenzy — {3}{R} Enchantment (GRN #99).
 *
 *   You may look at the top card of your library any time.
 *   You may play lands and cast spells from the top of your library.
 *   You can't play lands or cast spells from your hand.
 *   {3}{R}: Destroy this enchantment.
 *
 * Pinned: the top card is castable/playable, the hand is locked for both spells and lands (the
 * land lock is filtered to cards in hand, so it must not also forbid the land on top), and the
 * self-destroy ability lifts the lock.
 */
class ExperimentalFrenzyScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + ExperimentalFrenzy)
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.offers(player: EntityId, cardId: EntityId): Boolean =
        legalActions(player).any {
            (it.action as? PlayLand)?.cardId == cardId || (it.action as? CastSpell)?.cardId == cardId
        }

    test("casts a spell from the top of the library, but not the same spell from hand") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        driver.putPermanentOnBattlefield(me, "Experimental Frenzy")
        driver.giveMana(me, Color.GREEN, 4)

        val handBears = driver.putCardInHand(me, "Grizzly Bears")
        val topBears = driver.putCardOnTopOfLibrary(me, "Grizzly Bears")

        driver.offers(me, handBears) shouldBe false
        driver.castSpell(me, handBears).outcome.shouldBeInstanceOf<Outcome.Rejected>()

        driver.offers(me, topBears) shouldBe true
        driver.castSpell(me, topBears).outcome shouldBe Outcome.Done
        driver.bothPass()
        driver.findPermanent(me, "Grizzly Bears") shouldBe topBears
    }

    test("plays a land from the top of the library, but not one from hand") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        driver.putPermanentOnBattlefield(me, "Experimental Frenzy")

        val handForest = driver.putCardInHand(me, "Forest")
        val topForest = driver.putCardOnTopOfLibrary(me, "Forest")

        driver.offers(me, handForest) shouldBe false
        driver.playLand(me, handForest).outcome.shouldBeInstanceOf<Outcome.Rejected>()
        driver.findPermanent(me, "Forest") shouldBe null

        driver.offers(me, topForest) shouldBe true
        driver.playLand(me, topForest).outcome shouldBe Outcome.Done
        driver.findPermanent(me, "Forest") shouldBe topForest
    }

    test("the lock only binds its controller") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        driver.putPermanentOnBattlefield(opponent, "Experimental Frenzy")

        val handForest = driver.putCardInHand(me, "Forest")
        driver.offers(me, handForest) shouldBe true
        driver.playLand(me, handForest).outcome shouldBe Outcome.Done
    }

    test("{3}{R}: destroying it lifts the hand lock") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val frenzy = driver.putPermanentOnBattlefield(me, "Experimental Frenzy")
        val handForest = driver.putCardInHand(me, "Forest")
        driver.offers(me, handForest) shouldBe false

        driver.giveMana(me, Color.RED, 4)
        val destroy = ExperimentalFrenzy.activatedAbilities.single()
        driver.submit(ActivateAbility(me, frenzy, destroy.id)).outcome shouldBe Outcome.Done
        driver.bothPass()

        driver.findPermanent(me, "Experimental Frenzy") shouldBe null
        driver.getGraveyardCardNames(me).contains("Experimental Frenzy") shouldBe true
        driver.offers(me, handForest) shouldBe true
        driver.playLand(me, handForest).outcome shouldBe Outcome.Done
        driver.findPermanent(me, "Forest") shouldBe handForest
    }
})
