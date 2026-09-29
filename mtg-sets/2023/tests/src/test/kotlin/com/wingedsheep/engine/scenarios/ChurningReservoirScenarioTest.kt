package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.one.cards.ChurningReservoir
import com.wingedsheep.mtg.sets.definitions.one.cards.GlistenerSeer
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Churning Reservoir (ONE #127) — {R} Artifact.
 *
 * "At the beginning of your upkeep, put an oil counter on another target nontoken artifact or
 *  creature you control.
 *  {2}, {T}: Create a 1/1 red Phyrexian Goblin creature token. Activate only if an oil counter was
 *  removed from a permanent you controlled this turn or a permanent with an oil counter on it was
 *  put into a graveyard this turn."
 */
class ChurningReservoirScenarioTest : FunSpec({

    val tokenAbility = ChurningReservoir.activatedAbilities.first().id
    val seerAbility = GlistenerSeer.activatedAbilities.first().id

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(ChurningReservoir, GlistenerSeer))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun oil(driver: GameTestDriver, id: EntityId): Int =
        driver.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.OIL) ?: 0

    fun giveOil(driver: GameTestDriver, id: EntityId, amount: Int) {
        driver.replaceState(driver.state.updateEntity(id) { c ->
            c.with((c.get<CountersComponent>() ?: CountersComponent()).withAdded(CounterType.OIL, amount))
        })
    }

    /** A Glistener Seer with oil on it, ready to tap. */
    fun seerWithOil(driver: GameTestDriver, player: EntityId): EntityId {
        val seer = driver.putCreatureOnBattlefield(player, "Glistener Seer")
        driver.removeSummoningSickness(seer)
        giveOil(driver, seer, 2)
        return seer
    }

    fun goblins(driver: GameTestDriver, player: EntityId): Int =
        driver.state.getBattlefield().count { id ->
            driver.state.projectedState.getController(id) == player &&
                driver.state.getEntity(id)?.get<CardComponent>()?.typeLine?.subtypes
                    ?.any { it.value == "Goblin" } == true
        }

    fun activateReservoir(driver: GameTestDriver, reservoir: EntityId) =
        ActivateAbility(playerId = driver.player1, sourceId = reservoir, abilityId = tokenAbility)

    test("can't be activated with no oil history this turn") {
        val driver = newDriver()
        val reservoir = driver.putPermanentOnBattlefield(driver.player1, "Churning Reservoir")
        driver.giveMana(driver.player1, Color.RED, 2)

        driver.submitExpectFailure(activateReservoir(driver, reservoir))
        goblins(driver, driver.player1) shouldBe 0
    }

    test("an oil counter removed from your permanent as a cost unlocks it") {
        val driver = newDriver()
        val p1 = driver.player1
        val reservoir = driver.putPermanentOnBattlefield(p1, "Churning Reservoir")
        val seer = seerWithOil(driver, p1)

        driver.submitSuccess(ActivateAbility(playerId = p1, sourceId = seer, abilityId = seerAbility))
        oil(driver, seer) shouldBe 1
        driver.bothPass()
        while (driver.pendingDecision != null) driver.autoResolveDecision()

        driver.giveMana(p1, Color.RED, 2)
        driver.submitSuccess(activateReservoir(driver, reservoir))
        driver.bothPass()
        goblins(driver, p1) shouldBe 1
    }

    test("an oil counter removed from an opponent's permanent doesn't count for you") {
        val driver = newDriver()
        val p1 = driver.player1
        val p2 = driver.player2
        val reservoir = driver.putPermanentOnBattlefield(p1, "Churning Reservoir")
        val theirSeer = seerWithOil(driver, p2)

        // The opponent spends oil at instant speed on your turn.
        driver.passPriority(p1)
        driver.submitSuccess(ActivateAbility(playerId = p2, sourceId = theirSeer, abilityId = seerAbility))
        oil(driver, theirSeer) shouldBe 1
        driver.bothPass()
        while (driver.pendingDecision != null) driver.autoResolveDecision()

        driver.giveMana(p1, Color.RED, 2)
        driver.submitExpectFailure(activateReservoir(driver, reservoir))
    }

    test("any player's permanent put into a graveyard with an oil counter unlocks it") {
        val driver = newDriver()
        val p1 = driver.player1
        val p2 = driver.player2
        val reservoir = driver.putPermanentOnBattlefield(p1, "Churning Reservoir")
        val theirSeer = seerWithOil(driver, p2)

        val bolt = driver.putCardInHand(p1, "Lightning Bolt")
        driver.giveMana(p1, Color.RED, 1)
        driver.castSpellWithTargets(p1, bolt, listOf(ChosenTarget.Permanent(theirSeer))).error shouldBe null
        driver.bothPass()
        driver.assertInGraveyard(p2, "Glistener Seer")

        driver.giveMana(p1, Color.RED, 2)
        driver.submitSuccess(activateReservoir(driver, reservoir))
        driver.bothPass()
        goblins(driver, p1) shouldBe 1
    }

    test("a permanent put into a graveyard without an oil counter doesn't unlock it") {
        val driver = newDriver()
        val p1 = driver.player1
        val reservoir = driver.putPermanentOnBattlefield(p1, "Churning Reservoir")
        val seer = driver.putCreatureOnBattlefield(driver.player2, "Glistener Seer")

        val bolt = driver.putCardInHand(p1, "Lightning Bolt")
        driver.giveMana(p1, Color.RED, 1)
        driver.castSpellWithTargets(p1, bolt, listOf(ChosenTarget.Permanent(seer))).error shouldBe null
        driver.bothPass()
        driver.assertInGraveyard(driver.player2, "Glistener Seer")

        driver.giveMana(p1, Color.RED, 2)
        driver.submitExpectFailure(activateReservoir(driver, reservoir))
    }

    test("the history is this turn's only, and the upkeep trigger oils another nontoken permanent") {
        val driver = newDriver()
        val p1 = driver.player1
        val reservoir = driver.putPermanentOnBattlefield(p1, "Churning Reservoir")
        val seer = seerWithOil(driver, p1)

        driver.submitSuccess(ActivateAbility(playerId = p1, sourceId = seer, abilityId = seerAbility))
        driver.bothPass()
        while (driver.pendingDecision != null) driver.autoResolveDecision()

        // Through the opponent's turn to our next upkeep: the trigger's only legal target is the Seer.
        driver.passPriorityUntil(Step.UPKEEP)
        driver.passPriorityUntil(Step.DRAW)
        driver.passPriorityUntil(Step.UPKEEP)
        driver.activePlayer shouldBe p1
        if (driver.pendingDecision != null) driver.submitTargetSelection(p1, listOf(seer))
        driver.bothPass()
        oil(driver, seer) shouldBe 2
        oil(driver, reservoir) shouldBe 0

        // Last turn's removal no longer counts, and putting a counter on isn't removing one.
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.giveMana(p1, Color.RED, 2)
        driver.submitExpectFailure(activateReservoir(driver, reservoir))
    }
})
