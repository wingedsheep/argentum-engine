package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.one.cards.MindspliceApparatus
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Mindsplice Apparatus (ONE #63) — {3}{U} Artifact.
 *
 * "Flash
 *  At the beginning of your upkeep, put an oil counter on this artifact.
 *  Instant and sorcery spells you cast cost {1} less to cast for each oil counter on this artifact."
 */
class MindspliceApparatusScenarioTest : FunSpec({

    val TestSorcery = card("Test Ponder Sorcery") {
        manaCost = "{3}{U}"
        typeLine = "Sorcery"
        spell { effect = Effects.DrawCards(1) }
    }

    val TestCreature = card("Test Blue Golem") {
        manaCost = "{3}{U}"
        typeLine = "Artifact Creature — Golem"
        power = 2
        toughness = 2
    }

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(MindspliceApparatus, TestSorcery, TestCreature))
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun oil(driver: GameTestDriver, id: EntityId): Int =
        driver.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.OIL) ?: 0

    test("puts an oil counter on itself only at the beginning of its controller's upkeep") {
        val driver = newDriver()
        val p1 = driver.player1
        val apparatus = driver.putPermanentOnBattlefield(p1, "Mindsplice Apparatus")

        // Opponent's upkeep: no trigger.
        driver.passPriorityUntil(Step.UPKEEP)
        driver.activePlayer shouldNotBe p1
        oil(driver, apparatus) shouldBe 0

        // Controller's upkeep: trigger goes on the stack and resolves.
        driver.passPriorityUntil(Step.DRAW)
        driver.passPriorityUntil(Step.UPKEEP)
        driver.activePlayer shouldBe p1
        driver.bothPass()
        oil(driver, apparatus) shouldBe 1
    }

    test("instant and sorcery spells cost {1} less per oil counter") {
        val driver = newDriver()
        val p1 = driver.player1
        val apparatus = driver.putPermanentOnBattlefield(p1, "Mindsplice Apparatus")
        driver.addComponent(apparatus, CountersComponent(mapOf(CounterType.OIL to 2)))

        val sorcery = driver.putCardInHand(p1, "Test Ponder Sorcery")
        driver.giveMana(p1, Color.BLUE, 1)
        driver.giveColorlessMana(p1, 1)
        driver.castSpell(p1, sorcery).error shouldBe null
    }

    test("without oil counters there is no discount") {
        val driver = newDriver()
        val p1 = driver.player1
        driver.putPermanentOnBattlefield(p1, "Mindsplice Apparatus")

        val sorcery = driver.putCardInHand(p1, "Test Ponder Sorcery")
        driver.giveMana(p1, Color.BLUE, 1)
        driver.giveColorlessMana(p1, 1)
        driver.castSpell(p1, sorcery).error shouldNotBe null
    }

    test("discount never reduces colored mana") {
        val driver = newDriver()
        val p1 = driver.player1
        val apparatus = driver.putPermanentOnBattlefield(p1, "Mindsplice Apparatus")
        driver.addComponent(apparatus, CountersComponent(mapOf(CounterType.OIL to 6)))

        val sorcery = driver.putCardInHand(p1, "Test Ponder Sorcery")
        driver.giveColorlessMana(p1, 1)
        driver.castSpell(p1, sorcery).error shouldNotBe null

        driver.giveMana(p1, Color.BLUE, 1)
        driver.castSpell(p1, sorcery).error shouldBe null
    }

    test("non-instant, non-sorcery spells are not discounted") {
        val driver = newDriver()
        val p1 = driver.player1
        val apparatus = driver.putPermanentOnBattlefield(p1, "Mindsplice Apparatus")
        driver.addComponent(apparatus, CountersComponent(mapOf(CounterType.OIL to 3)))

        val creature = driver.putCardInHand(p1, "Test Blue Golem")
        driver.giveMana(p1, Color.BLUE, 1)
        driver.castSpell(p1, creature).error shouldNotBe null
    }

    test("an opponent's spells are not discounted") {
        val driver = newDriver()
        val p2 = driver.player2
        val apparatus = driver.putPermanentOnBattlefield(driver.player1, "Mindsplice Apparatus")
        driver.addComponent(apparatus, CountersComponent(mapOf(CounterType.OIL to 3)))

        // Advance to the opponent's own main phase.
        driver.passPriorityUntil(Step.UPKEEP)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.activePlayer shouldBe p2

        val sorcery = driver.putCardInHand(p2, "Test Ponder Sorcery")
        driver.giveMana(p2, Color.BLUE, 1)
        driver.castSpell(p2, sorcery).error shouldNotBe null
    }
})
