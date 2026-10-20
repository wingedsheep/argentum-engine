package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.avr.cards.Cloudshift
import com.wingedsheep.mtg.sets.definitions.leg.cards.Stangg
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Stangg's two linked clauses: the Twin goes when Stangg goes, and Stangg goes when the Twin goes.
 * The flicker cases are the ones worth proving — each clause watches the object it was created
 * for, so a Stangg that comes back as a new object keeps its new Twin and isn't sacrificed for the
 * old one (CR 400.7).
 */
class StanggScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(Stangg)
        driver.registerCard(Cloudshift)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.resolveAll() {
        var guard = 0
        while (guard++ < 20 && (state.stack.isNotEmpty() || pendingDecision != null)) {
            if (pendingDecision != null) autoResolveDecision() else bothPass()
        }
    }

    fun GameTestDriver.twins(player: EntityId) =
        getPermanents(player).filter { getCardName(it) == "Stangg Twin" }

    fun GameTestDriver.castStangg(me: EntityId): EntityId {
        val card = putCardInHand(me, "Stangg")
        giveMana(me, Color.RED, 1)
        giveMana(me, Color.GREEN, 1)
        giveColorlessMana(me, 4)
        castSpell(me, card)
        resolveAll()
        return findPermanent(me, "Stangg")!!
    }

    fun GameTestDriver.cloudshift(me: EntityId, target: EntityId) {
        val spell = putCardInHand(me, "Cloudshift")
        giveMana(me, Color.WHITE, 1)
        castSpell(me, spell, listOf(target))
        resolveAll()
    }

    test("Stangg enters with a legendary 3/4 red and green Stangg Twin") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        driver.castStangg(me)

        val twin = driver.twins(me).single()
        driver.state.projectedState.getPower(twin) shouldBe 3
        driver.state.projectedState.getToughness(twin) shouldBe 4
    }

    test("Stangg is sacrificed when the Twin leaves the battlefield") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        driver.castStangg(me)

        // A flickered token ceases to exist, so the Twin leaves for good.
        driver.cloudshift(me, driver.twins(me).single())

        withClue("the Twin is gone") { driver.twins(me) shouldBe emptyList() }
        withClue("its departure took Stangg with it") { driver.findPermanent(me, "Stangg") shouldBe null }
    }

    test("a flickered Stangg exiles its old Twin, keeps the new one, and isn't sacrificed for the old") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val stangg = driver.castStangg(me)
        val oldTwin = driver.twins(me).single()

        driver.cloudshift(me, stangg)

        withClue("the returned Stangg stays — the old Twin's leaving watched the old Stangg") {
            driver.findPermanent(me, "Stangg") shouldNotBe null
        }
        val newTwin = driver.twins(me).single()
        withClue("the old Twin was exiled; the new Stangg made a new one") { newTwin shouldNotBe oldTwin }

        // The new pair is linked: the new Twin leaving still sacrifices the new Stangg.
        driver.cloudshift(me, newTwin)
        driver.findPermanent(me, "Stangg") shouldBe null
    }
})
