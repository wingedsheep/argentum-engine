package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.handlers.continuations.entityIdToChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.avr.cards.Cloudshift
import com.wingedsheep.mtg.sets.definitions.drk.cards.WarBarge
import com.wingedsheep.mtg.sets.definitions.lea.cards.DrudgeSkeletons
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import com.wingedsheep.engine.core.Outcome

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
        driver.registerCard(Cloudshift)
        driver.registerCard(DrudgeSkeletons)
        return driver
    }

    fun GameTestDriver.resolveAll() {
        var guard = 0
        while (guard++ < 20 && (state.stack.isNotEmpty() || pendingDecision != null)) {
            if (pendingDecision != null) autoResolveDecision() else bothPass()
        }
    }

    fun GameTestDriver.ferry(me: EntityId, barge: EntityId, passenger: EntityId) {
        giveColorlessMana(me, 3)
        submit(
            ActivateAbility(
                playerId = me,
                sourceId = barge,
                abilityId = abilityId,
                targets = listOf(entityIdToChosenTarget(state, passenger)),
            )
        ).outcome shouldBe Outcome.Done
        resolveAll()
    }

    fun GameTestDriver.sink(me: EntityId, barge: EntityId) {
        giveMana(me, Color.RED, 2)
        val shatter = putCardInHand(me, "Shatter")
        castSpell(me, shatter, listOf(barge))
        resolveAll()
        findPermanent(me, "War Barge") shouldBe null
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
        ).outcome shouldBe Outcome.Done
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
        ).outcome shouldBe Outcome.Done
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
        ).outcome shouldBe Outcome.Done
        driver.bothPass()

        driver.passPriorityUntil(Step.END)
        driver.bothPass()

        withClue("nothing left the battlefield, so nothing drowns") {
            (driver.findPermanent(me, "Grizzly Bears") != null) shouldBe true
        }
        passenger shouldBe passenger
    }

    test("a passenger that left and came back is a new object the Barge no longer drowns") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        val me = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val barge = driver.putPermanentOnBattlefield(me, "War Barge")
        val passenger = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        driver.ferry(me, barge, passenger)

        driver.giveMana(me, Color.WHITE, 1)
        driver.castSpell(me, driver.putCardInHand(me, "Cloudshift"), listOf(passenger))
        driver.resolveAll()

        driver.sink(me, barge)
        withClue("the returned Bears are not the creature the Barge carried (CR 400.7)") {
            (driver.findPermanent(me, "Grizzly Bears") != null) shouldBe true
        }
    }

    test("a passenger destroyed this way can't be regenerated") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        val me = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val barge = driver.putPermanentOnBattlefield(me, "War Barge")
        val skeletons = driver.putCreatureOnBattlefield(me, "Drudge Skeletons")
        driver.ferry(me, barge, skeletons)

        driver.giveMana(me, Color.BLACK, 1)
        driver.submit(
            ActivateAbility(
                playerId = me,
                sourceId = skeletons,
                abilityId = DrudgeSkeletons.activatedAbilities.first().id,
            )
        ).outcome shouldBe Outcome.Done
        driver.resolveAll()

        driver.sink(me, barge)
        withClue("the regeneration shield doesn't save it") {
            driver.findPermanent(me, "Drudge Skeletons") shouldBe null
        }
    }
})
