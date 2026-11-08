package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.j25.cards.EverethViceroyOfPlunder
import com.wingedsheep.mtg.sets.tokens.PredefinedTokens
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Evereth, Viceroy of Plunder (Foundations Jumpstart #41).
 *
 * Sacrifice another creature or artifact: Put a +1/+1 counter on Evereth. If the sacrificed
 * permanent was a Treasure, Evereth gains lifelink until end of turn. Activate only as a sorcery.
 * When Evereth dies, you may pay {1}{B/R}. When you do, Evereth deals damage equal to its power to
 * each opponent.
 *
 * Covers the Treasure rider both ways (read off the cost-sacrificed snapshot), and the reflexive
 * dies trigger reading Evereth's last-known power — counters included — once it is in the
 * graveyard, plus declining the payment.
 */
class EverethViceroyOfPlunderScenarioTest : FunSpec({

    val abilityId = EverethViceroyOfPlunder.activatedAbilities.first().id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(EverethViceroyOfPlunder, PredefinedTokens.Treasure))
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun sacrifice(driver: GameTestDriver, player: EntityId, evereth: EntityId, fodder: EntityId) {
        val result = driver.submit(
            ActivateAbility(
                playerId = player,
                sourceId = evereth,
                abilityId = abilityId,
                costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(fodder))
            )
        )
        result.outcome shouldBe Outcome.Done
        while (driver.state.stack.isNotEmpty()) driver.bothPass()
    }

    fun plusOneCounters(driver: GameTestDriver, id: EntityId): Int =
        driver.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    fun settle(driver: GameTestDriver) {
        var guard = 0
        while (driver.state.stack.isNotEmpty() && driver.state.pendingDecision == null && guard++ < 20) {
            driver.bothPass()
        }
    }

    /**
     * Sacrifice a creature (+1/+1 counter, Evereth now 3/3), then Bolt Evereth so it dies. The
     * {1}{B/R} is floated before the Bolt: the "you may pay" is only offered when it's affordable
     * as the dies trigger resolves.
     */
    fun growThenKill(driver: GameTestDriver, player: EntityId): EntityId {
        val evereth = driver.putCreatureOnBattlefield(player, "Evereth, Viceroy of Plunder")
        val bears = driver.putCreatureOnBattlefield(player, "Grizzly Bears")
        sacrifice(driver, player, evereth, bears)
        plusOneCounters(driver, evereth) shouldBe 1

        val bolt = driver.putCardInHand(player, "Lightning Bolt")
        driver.giveMana(player, Color.RED, 1)
        driver.giveMana(player, Color.BLACK, 2)
        driver.castSpell(player, bolt, listOf(evereth))
        settle(driver)
        driver.findPermanent(player, "Evereth, Viceroy of Plunder") shouldBe null
        return evereth
    }

    test("sacrificing a Treasure adds a counter and grants lifelink") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        val evereth = driver.putCreatureOnBattlefield(you, "Evereth, Viceroy of Plunder")
        val treasure = driver.putPermanentOnBattlefield(you, "Treasure")

        sacrifice(driver, you, evereth, treasure)

        plusOneCounters(driver, evereth) shouldBe 1
        driver.state.projectedState.hasKeyword(evereth, Keyword.LIFELINK) shouldBe true
    }

    test("sacrificing a non-Treasure creature adds a counter but no lifelink") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        val evereth = driver.putCreatureOnBattlefield(you, "Evereth, Viceroy of Plunder")
        val bears = driver.putCreatureOnBattlefield(you, "Grizzly Bears")

        sacrifice(driver, you, evereth, bears)

        plusOneCounters(driver, evereth) shouldBe 1
        driver.state.projectedState.hasKeyword(evereth, Keyword.LIFELINK) shouldBe false
    }

    test("paying {1}{B/R} when it dies deals its last-known power to each opponent") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)
        growThenKill(driver, you)

        driver.submitYesNo(you, true)
        while (driver.state.stack.isNotEmpty()) driver.bothPass()

        // Evereth left the battlefield as a 3/3 (base 2/2 plus one +1/+1 counter).
        driver.getLifeTotal(opponent) shouldBe 17
        driver.getLifeTotal(you) shouldBe 20
    }

    test("declining the payment deals no damage") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)
        growThenKill(driver, you)

        driver.submitYesNo(you, false)
        while (driver.state.stack.isNotEmpty()) driver.bothPass()

        driver.getLifeTotal(opponent) shouldBe 20
    }
})
