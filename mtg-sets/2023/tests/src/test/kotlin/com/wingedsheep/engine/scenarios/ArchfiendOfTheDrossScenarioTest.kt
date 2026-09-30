package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.m13.cards.Murder
import com.wingedsheep.mtg.sets.definitions.one.cards.ArchfiendOfTheDross
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Archfiend of the Dross (ONE #82) — {2}{B}{B} 6/6 Creature — Phyrexian Demon.
 *
 * "Flying. This creature enters with four oil counters on it. At the beginning of your upkeep,
 *  remove an oil counter from this creature. Then if it has no oil counters on it, you lose the
 *  game. Whenever a creature an opponent controls dies, its controller loses 2 life."
 */
class ArchfiendOfTheDrossScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(ArchfiendOfTheDross, Murder))
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun oil(driver: GameTestDriver, id: EntityId): Int =
        driver.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.OIL) ?: 0

    fun setOil(driver: GameTestDriver, id: EntityId, count: Int) {
        driver.replaceState(driver.state.updateEntity(id) { it.with(CountersComponent(mapOf(CounterType.OIL to count))) })
    }

    fun castArchfiend(driver: GameTestDriver): EntityId {
        val p1 = driver.player1
        val card = driver.putCardInHand(p1, "Archfiend of the Dross")
        driver.giveMana(p1, Color.BLACK, 2)
        driver.giveColorlessMana(p1, 2)
        driver.castSpell(p1, card).outcome shouldBe Outcome.Done
        driver.bothPass()
        return driver.findPermanent(p1, "Archfiend of the Dross")!!
    }

    /** Through the opponent's turn to P1's next upkeep, with the oil trigger on the stack. */
    fun toNextUpkeep(driver: GameTestDriver) {
        driver.passPriorityUntil(Step.UPKEEP)
        driver.passPriorityUntil(Step.DRAW)
        driver.passPriorityUntil(Step.UPKEEP)
        driver.state.activePlayerId shouldBe driver.player1
        driver.state.stack.size shouldBe 1
    }

    test("enters with four oil counters and the upkeep trigger removes one") {
        val driver = newDriver()
        val archfiend = castArchfiend(driver)
        oil(driver, archfiend) shouldBe 4

        toNextUpkeep(driver)
        driver.bothPass()

        oil(driver, archfiend) shouldBe 3
        driver.state.gameOver shouldBe false
    }

    test("removing the last oil counter loses you the game") {
        val driver = newDriver()
        val archfiend = castArchfiend(driver)
        setOil(driver, archfiend, 1)

        toNextUpkeep(driver)
        driver.bothPass()

        driver.state.gameOver shouldBe true
        driver.state.winnerId shouldBe driver.player2
    }

    test("gone before the trigger resolves: its last-known oil counters decide, so one left means no loss") {
        val driver = newDriver()
        val archfiend = castArchfiend(driver)
        setOil(driver, archfiend, 1)

        toNextUpkeep(driver)
        val murder = driver.putCardInHand(driver.player1, "Murder")
        driver.giveMana(driver.player1, Color.BLACK, 2)
        driver.giveColorlessMana(driver.player1, 1)
        driver.castSpellWithTargets(driver.player1, murder, listOf(ChosenTarget.Permanent(archfiend))).error shouldBe null
        driver.bothPass() // Murder resolves
        driver.assertInGraveyard(driver.player1, "Archfiend of the Dross")
        driver.state.stack.size shouldBe 1
        driver.bothPass() // the oil trigger resolves off the battlefield

        driver.state.gameOver shouldBe false
    }

    test("an opponent's creature dying costs its controller 2 life; your own does not") {
        val driver = newDriver()
        castArchfiend(driver)
        val theirs = driver.putCreatureOnBattlefield(driver.player2, "Savannah Lions")
        val mine = driver.putCreatureOnBattlefield(driver.player1, "Centaur Courser")

        fun murder(target: EntityId) {
            val spell = driver.putCardInHand(driver.player1, "Murder")
            driver.giveMana(driver.player1, Color.BLACK, 2)
            driver.giveColorlessMana(driver.player1, 1)
            driver.castSpellWithTargets(driver.player1, spell, listOf(ChosenTarget.Permanent(target))).error shouldBe null
            driver.bothPass()
            if (driver.state.stack.isNotEmpty()) driver.bothPass()
        }

        murder(theirs)
        driver.getLifeTotal(driver.player2) shouldBe 18
        driver.getLifeTotal(driver.player1) shouldBe 20

        murder(mine)
        driver.getLifeTotal(driver.player1) shouldBe 20
        driver.findPermanent(driver.player1, "Archfiend of the Dross") shouldNotBe null
    }
})
