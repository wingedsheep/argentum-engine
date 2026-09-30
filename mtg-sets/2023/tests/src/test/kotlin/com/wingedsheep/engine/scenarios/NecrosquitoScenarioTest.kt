package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lea.cards.Shatter
import com.wingedsheep.mtg.sets.definitions.one.cards.Necrosquito
import com.wingedsheep.mtg.sets.definitions.wth.cards.MindStone
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Necrosquito (ONE #100) — {3}{B} 0/0 Creature — Phyrexian Insect.
 *
 * "Flying. This creature enters with two oil counters on it. This creature gets +1/+1 for each oil
 *  counter on it. Whenever another creature or artifact you control is put into a graveyard from
 *  the battlefield, put an oil counter on this creature."
 */
class NecrosquitoScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(Necrosquito, MindStone, Shatter))
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun oil(driver: GameTestDriver, id: EntityId): Int =
        driver.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.OIL) ?: 0

    fun castNecrosquito(driver: GameTestDriver): EntityId {
        val p1 = driver.player1
        val card = driver.putCardInHand(p1, "Necrosquito")
        driver.giveMana(p1, Color.BLACK, 1)
        driver.giveColorlessMana(p1, 3)
        driver.castSpell(p1, card).outcome shouldBe Outcome.Done
        driver.bothPass()
        val squito = driver.findPermanent(p1, "Necrosquito")
        squito shouldNotBe null
        return squito!!
    }

    fun bolt(driver: GameTestDriver, target: EntityId) {
        val bolt = driver.putCardInHand(driver.player1, "Lightning Bolt")
        driver.giveMana(driver.player1, Color.RED, 1)
        driver.castSpellWithTargets(driver.player1, bolt, listOf(ChosenTarget.Permanent(target))).error shouldBe null
        driver.bothPass()
        while (driver.pendingDecision != null) driver.autoResolveDecision()
        if (driver.state.stack.isNotEmpty()) driver.bothPass()
    }

    fun shatter(driver: GameTestDriver, target: EntityId) {
        val spell = driver.putCardInHand(driver.player1, "Shatter")
        driver.giveMana(driver.player1, Color.RED, 2)
        driver.castSpellWithTargets(driver.player1, spell, listOf(ChosenTarget.Permanent(target))).error shouldBe null
        driver.bothPass()
        while (driver.pendingDecision != null) driver.autoResolveDecision()
        if (driver.state.stack.isNotEmpty()) driver.bothPass()
    }

    test("enters with two oil counters and is a 2/2") {
        val driver = newDriver()
        val squito = castNecrosquito(driver)
        oil(driver, squito) shouldBe 2
        driver.state.projectedState.getPower(squito) shouldBe 2
        driver.state.projectedState.getToughness(squito) shouldBe 2
    }

    test("another creature you control dying adds an oil counter and grows it; an opponent's does not") {
        val driver = newDriver()
        val squito = castNecrosquito(driver)
        val mine = driver.putCreatureOnBattlefield(driver.player1, "Centaur Courser")
        val theirs = driver.putCreatureOnBattlefield(driver.player2, "Savannah Lions")

        bolt(driver, mine)
        driver.assertInGraveyard(driver.player1, "Centaur Courser")
        oil(driver, squito) shouldBe 3
        driver.state.projectedState.getPower(squito) shouldBe 3
        driver.state.projectedState.getToughness(squito) shouldBe 3

        bolt(driver, theirs)
        driver.assertInGraveyard(driver.player2, "Savannah Lions")
        oil(driver, squito) shouldBe 3
    }

    test("a noncreature artifact you control going to the graveyard also adds an oil counter") {
        val driver = newDriver()
        val squito = castNecrosquito(driver)
        val stone = driver.putPermanentOnBattlefield(driver.player1, "Mind Stone")

        shatter(driver, stone)
        driver.assertInGraveyard(driver.player1, "Mind Stone")
        oil(driver, squito) shouldBe 3
    }
})
