package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.khm.cards.WeatheredRunestone
import com.wingedsheep.mtg.sets.definitions.mbs.cards.GreenSunsZenith
import com.wingedsheep.mtg.sets.definitions.mir.cards.RampantGrowth
import com.wingedsheep.mtg.sets.definitions.ody.cards.Zombify
import com.wingedsheep.mtg.sets.definitions.ons.cards.FutureSight
import com.wingedsheep.mtg.sets.definitions.tsp.cards.ThinkTwice
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Weathered Runestone (KHM #247) — {2} Artifact.
 *
 * "Nonland permanent cards in graveyards and libraries can't enter the battlefield.
 *  Players can't cast spells from graveyards or libraries."
 *
 * The first card to point the entry lock and the cast lock at the **library**, and the first to
 * scope the entry lock to *nonland* permanents — lands from either zone still enter (ruling
 * 2021-02-05), and playing a land is not casting a spell.
 */
class WeatheredRunestoneScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCards(
            listOf(WeatheredRunestone, GreenSunsZenith, RampantGrowth, Zombify, FutureSight, ThinkTwice)
        )
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.settle() {
        var guard = 0
        while (guard++ < 30) {
            when {
                isPaused -> autoResolveDecision()
                state.stack.isNotEmpty() -> bothPass()
                else -> break
            }
        }
    }

    fun GameTestDriver.castableCards(player: EntityId): Set<EntityId> =
        legalActions(player).filter { it.affordable }.mapNotNull { (it.action as? CastSpell)?.cardId }.toSet()

    test("a creature card found in a library can't be put onto the battlefield") {
        val driver = newDriver()
        val me = driver.player1
        driver.putPermanentOnBattlefield(me, "Weathered Runestone")
        val bears = driver.putCardOnTopOfLibrary(me, "Grizzly Bears")

        val zenith = driver.putCardInHand(me, "Green Sun's Zenith")
        driver.giveMana(me, Color.GREEN, 3)
        driver.submit(
            CastSpell(me, zenith, xValue = 2, paymentStrategy = PaymentStrategy.FromPool)
        ).error shouldBe null
        driver.settle()

        withClue("the lock wins over the search's instruction; the card stays in the library") {
            driver.findPermanent(me, "Grizzly Bears") shouldBe null
            driver.state.getZone(ZoneKey(me, Zone.LIBRARY)) shouldContain bears
        }
    }

    test("a land card from a library still enters") {
        val driver = newDriver()
        val me = driver.player1
        driver.putPermanentOnBattlefield(me, "Weathered Runestone")
        val forest = driver.putCardOnTopOfLibrary(me, "Forest")

        val growth = driver.putCardInHand(me, "Rampant Growth")
        driver.giveMana(me, Color.GREEN, 2)
        driver.submit(CastSpell(me, growth, paymentStrategy = PaymentStrategy.FromPool)).error shouldBe null

        var guard = 0
        while (!driver.isPaused && driver.state.stack.isNotEmpty() && guard++ < 10) driver.bothPass()
        driver.submitCardSelection(me, listOf(forest))
        driver.settle()

        driver.state.getZone(ZoneKey(me, Zone.BATTLEFIELD)) shouldContain forest
    }

    test("a creature card can't be returned from a graveyard, until the Runestone leaves") {
        val driver = newDriver()
        val me = driver.player1
        val runestone = driver.putPermanentOnBattlefield(me, "Weathered Runestone")
        val courser = driver.putCardInGraveyard(me, "Centaur Courser")

        driver.giveMana(me, Color.BLACK, 8)
        val first = driver.putCardInHand(me, "Zombify")
        driver.castSpellWithTargets(me, first, listOf(ChosenTarget.Card(courser, me, Zone.GRAVEYARD)))
        driver.bothPass()

        driver.getGraveyard(me) shouldContain courser
        driver.findPermanent(me, "Centaur Courser") shouldBe null

        driver.moveToGraveyard(runestone)
        val second = driver.putCardInHand(me, "Zombify")
        driver.castSpellWithTargets(me, second, listOf(ChosenTarget.Card(courser, me, Zone.GRAVEYARD)))
        driver.bothPass()

        driver.findPermanent(me, "Centaur Courser") shouldBe courser
    }

    test("spells can't be cast from a library or a graveyard; the hand and land plays are unaffected") {
        val driver = newDriver()
        val me = driver.player1
        driver.putPermanentOnBattlefield(me, "Future Sight")
        driver.giveMana(me, Color.BLUE, 4)
        driver.giveMana(me, Color.RED, 1)
        val boltOnTop = driver.putCardOnTopOfLibrary(me, "Lightning Bolt")
        val flashback = driver.putCardInGraveyard(me, "Think Twice")
        val fromHand = driver.putCardInHand(me, "Think Twice")

        val before = driver.castableCards(me)
        before shouldContain boltOnTop
        before shouldContain flashback
        val flashbackCast = driver.legalActions(me).map { it.action }
            .filterIsInstance<CastSpell>().first { it.cardId == flashback }

        val runestone = driver.putPermanentOnBattlefield(me, "Weathered Runestone")
        val locked = driver.castableCards(me)
        locked shouldNotContain boltOnTop
        locked shouldNotContain flashback
        locked shouldContain fromHand
        // The handler refuses it too, not only the legal-action list.
        driver.submitExpectFailure(flashbackCast)

        driver.moveToGraveyard(runestone)
        driver.castableCards(me) shouldContain boltOnTop
    }

    test("a land can still be played from the top of a library") {
        val driver = newDriver()
        val me = driver.player1
        driver.putPermanentOnBattlefield(me, "Future Sight")
        driver.putPermanentOnBattlefield(me, "Weathered Runestone")
        val forestOnTop = driver.putCardOnTopOfLibrary(me, "Forest")

        driver.playLand(me, forestOnTop).error shouldBe null
        driver.state.getZone(ZoneKey(me, Zone.BATTLEFIELD)) shouldContain forestOnTop
    }
})
