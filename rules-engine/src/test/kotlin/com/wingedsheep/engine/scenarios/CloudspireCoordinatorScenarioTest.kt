package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dft.cards.CloudspireCoordinator
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.KeywordAbility
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Cloudspire Coordinator (DFT) — {R}{W} 3/1 Creature — Human Pilot
 *
 * "When this creature enters, scry 2.
 *  {T}: Create X 1/1 colorless Pilot creature tokens, where X is the number of Mounts and/or
 *  Vehicles that entered the battlefield under your control this turn. …"
 *
 * X comes from the per-player entry log, so these tests put the Mounts/Vehicles onto the
 * battlefield by *casting* them — the driver's direct-placement helpers bypass the tracker.
 */
class CloudspireCoordinatorScenarioTest : FunSpec({

    val tapAbilityId = CloudspireCoordinator.activatedAbilities[0].id

    val testMount = card("Test Mount") {
        manaCost = "{1}"
        typeLine = "Creature — Horse Mount"
        power = 1
        toughness = 1
    }

    val testVehicle = card("Test Vehicle") {
        manaCost = "{1}"
        typeLine = "Artifact — Vehicle"
        power = 2
        toughness = 2
        keywordAbility(KeywordAbility.crew(1))
    }

    // Synthetic: no printed card carries both subtypes, but the any-of count must still tally such
    // a permanent once rather than twice, so pin it directly.
    val testMountVehicle = card("Test Mount Vehicle") {
        manaCost = "{1}"
        typeLine = "Artifact Creature — Vehicle Mount"
        power = 2
        toughness = 2
    }

    val testBear = card("Test Bear") {
        manaCost = "{1}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(CloudspireCoordinator)
        listOf(testMount, testVehicle, testMountVehicle, testBear).forEach { driver.registerCard(it) }
        return driver
    }

    /** Cast [cardName] from hand and resolve the whole stack. */
    fun GameTestDriver.castPermanent(playerId: EntityId, cardName: String) {
        val cardId = putCardInHand(playerId, cardName)
        giveColorlessMana(playerId, 1)
        castSpell(playerId, cardId)
        var guard = 0
        while (stackSize > 0 && guard++ < 20) bothPass()
    }

    fun pilotTokens(driver: GameTestDriver, playerId: EntityId): List<EntityId> =
        driver.getCreatures(playerId).filter { driver.getCardName(it) == "Pilot Token" }

    fun GameTestDriver.setUpCoordinator(playerId: EntityId): EntityId {
        val coordinator = putCreatureOnBattlefield(playerId, "Cloudspire Coordinator")
        removeSummoningSickness(coordinator)
        return coordinator
    }

    test("X counts each Mount and each Vehicle that entered under your control this turn") {
        val driver = createDriver()
        driver.initMirrorMatch(Deck.of("Plains" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.player1

        val coordinator = driver.setUpCoordinator(me)
        driver.castPermanent(me, "Test Mount")
        driver.castPermanent(me, "Test Vehicle")

        driver.submitSuccess(ActivateAbility(playerId = me, sourceId = coordinator, abilityId = tapAbilityId))
        var guard = 0
        while (driver.stackSize > 0 && guard++ < 20) driver.bothPass()

        pilotTokens(driver, me).size shouldBe 2
    }

    test("a permanent that is both a Mount and a Vehicle counts once, not twice") {
        val driver = createDriver()
        driver.initMirrorMatch(Deck.of("Plains" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.player1

        val coordinator = driver.setUpCoordinator(me)
        driver.castPermanent(me, "Test Mount Vehicle")

        driver.submitSuccess(ActivateAbility(playerId = me, sourceId = coordinator, abilityId = tapAbilityId))
        var guard = 0
        while (driver.stackSize > 0 && guard++ < 20) driver.bothPass()

        pilotTokens(driver, me).size shouldBe 1
    }

    test("permanents that are neither a Mount nor a Vehicle are not counted") {
        val driver = createDriver()
        driver.initMirrorMatch(Deck.of("Plains" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.player1

        val coordinator = driver.setUpCoordinator(me)
        driver.castPermanent(me, "Test Bear")

        driver.submitSuccess(ActivateAbility(playerId = me, sourceId = coordinator, abilityId = tapAbilityId))
        var guard = 0
        while (driver.stackSize > 0 && guard++ < 20) driver.bothPass()

        pilotTokens(driver, me).size shouldBe 0
    }

    test("a Vehicle that entered and then died still counts — the entry is what's tracked") {
        val driver = createDriver()
        driver.initMirrorMatch(Deck.of("Plains" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.player1

        val coordinator = driver.setUpCoordinator(me)
        driver.castPermanent(me, "Test Vehicle")
        val vehicle = driver.getPermanents(me).single { driver.getCardName(it) == "Test Vehicle" }
        driver.moveToGraveyard(vehicle)

        driver.submitSuccess(ActivateAbility(playerId = me, sourceId = coordinator, abilityId = tapAbilityId))
        var guard = 0
        while (driver.stackSize > 0 && guard++ < 20) driver.bothPass()

        pilotTokens(driver, me).size shouldBe 1
    }
})
