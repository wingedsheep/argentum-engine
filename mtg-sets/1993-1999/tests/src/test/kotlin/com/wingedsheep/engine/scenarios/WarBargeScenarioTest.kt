package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.handlers.continuations.entityIdToChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.drk.cards.WarBarge
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for War Barge.
 *
 * The second sentence is the one worth proving: a delayed trigger watching *this artifact* leave,
 * scheduled per activation. So the test ferries a creature, sinks the Barge, and expects the
 * passenger to drown — and separately checks that a passenger whose Barge survives is fine, since a
 * watcher wired to the wrong event (or to no expiry) would kill it anyway.
 */
class WarBargeScenarioTest : FunSpec({

    val abilityId = WarBarge.activatedAbilities.first().id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(WarBarge)
        return driver
    }

    test("grants islandwalk, and the passenger drowns when the Barge sinks") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)

        val me = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val barge = driver.putPermanentOnBattlefield(me, "War Barge")
        val passenger = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        driver.giveColorlessMana(me, 3)

        driver.submit(
            ActivateAbility(
                playerId = me,
                sourceId = barge,
                abilityId = abilityId,
                targets = listOf(entityIdToChosenTarget(driver.state, passenger)),
            )
        ).isSuccess shouldBe true
        driver.bothPass()

        withClue("the passenger is aboard") {
            driver.state.projectedState.hasKeyword(passenger, Keyword.ISLANDWALK) shouldBe true
        }

        // Sink the Barge for real — a raw state edit would emit no zone-change event and the
        // delayed trigger would never see it leave.
        driver.giveMana(me, com.wingedsheep.sdk.core.Color.RED, 2)
        val shatter = driver.putCardInHand(me, "Shatter")
        driver.submit(
            CastSpell(
                playerId = me,
                cardId = shatter,
                targets = listOf(entityIdToChosenTarget(driver.state, barge)),
            )
        ).isSuccess shouldBe true
        var guard = 0
        while (guard++ < 10 && (driver.state.stack.isNotEmpty() || driver.pendingDecision != null)) {
            if (driver.pendingDecision != null) driver.autoResolveDecision() else driver.bothPass()
        }

        withClue("the Barge really left the battlefield") {
            driver.findPermanent(me, "War Barge") shouldBe null
        }
        withClue("the delayed trigger fired on the Barge leaving") {
            driver.findPermanent(me, "Grizzly Bears") shouldBe null
        }
    }

    test("a passenger whose Barge stays afloat is unharmed") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)

        val me = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val barge = driver.putPermanentOnBattlefield(me, "War Barge")
        val passenger = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        driver.giveColorlessMana(me, 3)

        driver.submit(
            ActivateAbility(
                playerId = me,
                sourceId = barge,
                abilityId = abilityId,
                targets = listOf(entityIdToChosenTarget(driver.state, passenger)),
            )
        ).isSuccess shouldBe true
        driver.bothPass()

        driver.passPriorityUntil(Step.END)
        driver.bothPass()

        withClue("nothing left the battlefield, so nothing drowns") {
            (driver.findPermanent(me, "Grizzly Bears") != null) shouldBe true
        }
        passenger shouldBe passenger
    }
})
